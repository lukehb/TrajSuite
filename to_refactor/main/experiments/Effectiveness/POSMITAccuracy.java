package onethreeseven.trajsuite.experiments.Effectiveness;

import onethreeseven.common.model.TimeCategoryPool;
import onethreeseven.common.util.FileUtil;
import onethreeseven.common.util.Maths;
import onethreeseven.datastructures.model.STStopTrajectory;
import onethreeseven.datastructures.model.SpatioCompositeTrajectory;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.stopmove.algorithm.POSMIT;
import onethreeseven.trajsuite.osm.algorithm.SeClust;
import onethreeseven.trajsuite.osm.data.SyntheticSemanticTrajectoryGenerator;
import onethreeseven.trajsuite.osm.model.SemanticTrajectory;
import onethreeseven.trajsuite.osm.model.StopEpisode;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Todo: write documentation
 *
 * @author Luke Bermingham
 */
public class POSMITAccuracy {

    private static final int minNoise = 1;
    private static final int noiseStep = 2;
    private static final int maxNoise = 25;

    private static final File osmFile = new File(FileUtil.makeAppDir("osm_extracts"), "beijing_china.osm.pbf");
    private static final int nPlaces = 100;
    private static final int nTrajs = 1;
    private static final int nEntriesPerEpisode = 20;
    private static final int nEpisodes = 1000;
    private static final long episodeDurationMillis = 100000L * nEntriesPerEpisode;
    private static final int durationMultiplier = 2;
    private static final AbstractGeographicProjection projection = new ProjectionEquirectangular();
    private static final TimeCategoryPool timeCategoryPool = TimeCategoryPool.TIMES_OF_DAY;

    //POSMIT
    private static final int posmitSearchRadius = 7;
    private static final double fixedStopVariance = 200;
    private static final boolean estimateStopVariance = false;
    private static final boolean estimateMinStopPr = false;
    private static final double posmitMinStopPr = 0.01;
    private static final POSMIT posmit = new POSMIT();

    //SeClust
    private static final SeClust seclust = new SeClust();

    public static void main(String[] args) {

        final StringBuilder output = new StringBuilder();

        final SyntheticSemanticTrajectoryGenerator gen = new SyntheticSemanticTrajectoryGenerator();

        output.append("Entries,Noise,Accuracy\n");

        for (int noise = minNoise; noise < maxNoise; noise+=noiseStep) {
            Map<String, SemanticTrajectory> syntheticTrajs = gen.makeTrajectories(projection,
                    osmFile,
                    nPlaces,
                    nTrajs,
                    nEpisodes,
                    nEntriesPerEpisode,
                    (double) noise,
                    LocalDateTime.now(),
                    episodeDurationMillis,
                    durationMultiplier,
                    timeCategoryPool
                    );

            int totalEntries = syntheticTrajs.values().stream().mapToInt(value -> value.size()).sum();
            System.out.println("Entries: " + totalEntries);

            double[] accuracies = new double[syntheticTrajs.size()];
            int i = 0;

            output.append(totalEntries).append(",");
            output.append(noise).append(",");

            for (SemanticTrajectory truthTraj : syntheticTrajs.values()) {

                double stopVariance = (estimateStopVariance) ? posmit.estimateStopVariance(truthTraj) : noise*2;

                System.out.println("Stop variance set to: " + stopVariance);

                double[] stopPrs = posmit.run(truthTraj, 7, stopVariance);

                double minStopPr = estimateMinStopPr ? posmit.estimateMinStopPr(stopPrs) : posmitMinStopPr;

                System.out.println("Min stop pr: " + minStopPr);

                STStopTrajectory stopTraj = posmit.toStopTrajectory(truthTraj, stopPrs, minStopPr);
                SpatioCompositeTrajectory<StopEpisode> stopEpisodes = seclust.run(stopTraj, episodeDurationMillis/2, episodeDurationMillis/2, timeCategoryPool);
                System.out.println("Stop eps found:" + stopEpisodes.size());
                //compress the semantic trajectory to get a ground truth of stops
                SyntheticSemanticTrajectoryGenerator.compressSemanticTrajectory(truthTraj);

                accuracies[i] += SimplificationSemanticAccuracy.calculateAccuracy(truthTraj, stopEpisodes);
                i++;
            }

            double meanAccuracy = Maths.mean(accuracies) * 100;
            output.append(meanAccuracy).append("\n");
            System.out.println("Accuracy: " + meanAccuracy);
        }

        System.out.println(output.toString());

    }

}
