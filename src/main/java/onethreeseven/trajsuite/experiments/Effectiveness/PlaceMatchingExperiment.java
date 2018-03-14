package onethreeseven.trajsuite.experiments.Effectiveness;

import onethreeseven.common.model.TimeCategoryPool;
import onethreeseven.common.util.FileUtil;
import onethreeseven.common.util.Maths;
import onethreeseven.datastructures.data.STTrajectoryParser;
import onethreeseven.datastructures.data.resolver.*;
import onethreeseven.datastructures.model.STStopTrajectory;
import onethreeseven.datastructures.model.STTrajectory;
import onethreeseven.datastructures.model.SpatioCompositeTrajectory;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.stopmove.algorithm.POSMIT;
import onethreeseven.trajsuite.osm.algorithm.HMMPlaceMatching;
import onethreeseven.trajsuite.osm.algorithm.IPlaceMatching;
import onethreeseven.trajsuite.osm.algorithm.SeClust;
import onethreeseven.trajsuite.osm.algorithm.SemanticTrajectoryBuilder;
import onethreeseven.trajsuite.osm.data.SemanticPlaceFieldsResolver;
import onethreeseven.trajsuite.osm.data.SemanticTrajectoryParser;
import onethreeseven.trajsuite.osm.model.SemanticTrajectory;
import onethreeseven.trajsuite.osm.model.StopEpisode;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Experiment to measure place-matching accuracy and running time.
 * @author Luke Bermingham
 */
public class PlaceMatchingExperiment {

    private final static File inputTrajs = new File(FileUtil.makeAppDir("traj"), "geolife_179.txt");
    private final static File truthTrajs = new File(FileUtil.makeAppDir("traj"), "synthetic_compressed_40000_noise_10.0.txt");
    private final static AbstractGeographicProjection projection = new ProjectionEquirectangular();
    private final static TimeCategoryPool timeCategories = TimeCategoryPool.TIMES_OF_DAY;

    //test all dataset sizes
    private final static boolean testAllTrajsIncrementally = true;

    //accuracy measurement
    private final static boolean doAccuracyMeasurement = false;

    //stop/move detection
    private final static POSMIT algoPosmit = new POSMIT();
    private final static int searchRadius = 7;
    private final static double stopVariance = 10;
    private final static double minStopPr = 0.8;

    //stop episodes conversion
    private final static SeClust converter = new SeClust();
    private final static long minMoveEpisodeDurationMillis = 120000L;
    private final static long stopEpisodeDurationMillis = 120000L;

    //place-matching
    private static final double homogeneityFactor = 0.5;
    private static final int matchingBufferMeters = 200;

    private static final IPlaceMatching matchingAlgo =
                new HMMPlaceMatching(matchingBufferMeters)
                .setQueryWebForBetterPlaces(false)
                .setOnlyExpandBest(false)
                .setHomogeneityFactor(homogeneityFactor);

    //private static final IPlaceMatching matchingAlgo = new NaivePlaceMatching();

    private final static SemanticTrajectoryBuilder semanticBuilder = new SemanticTrajectoryBuilder();
    private static final File osmFile = new File(FileUtil.makeAppDir("osm_extracts"), "beijing_china.osm.pbf");

    private static final StringBuilder output = new StringBuilder();

    public static void main(String[] args) {

        output.append("Entries, Stop Episode Clustering (ms), Place-matching (ms), Compression(%), Place-matching Accuracy (%)\n");

        if(testAllTrajsIncrementally){
            for (int i = 0; i < 179; i+=20) {
                Map<String, SemanticTrajectory> semanticTrajectories = transformIntoSemanticTrajectories(i);

                if(doAccuracyMeasurement){
                    double accuracy = computeMatchingAccuracy(semanticTrajectories);
                    output.append(accuracy);
                }

                output.append("\n");
            }
        }
        else{
            Map<String, SemanticTrajectory> semanticTrajectories = transformIntoSemanticTrajectories(Integer.MAX_VALUE);

            if(doAccuracyMeasurement){
                double accuracy = computeMatchingAccuracy(semanticTrajectories);
                output.append(accuracy);
            }

            output.append("\n");
        }

        System.out.println(output.toString());
    }

    private static double computeMatchingAccuracy(Map<String, SemanticTrajectory> semanticTrajectories){
        SemanticTrajectoryParser semanticParser = new SemanticTrajectoryParser(
                projection,
                new IdFieldResolver(0),
                new NumericFieldsResolver(1,2),
                new TemporalFieldResolver(3),
                new SemanticPlaceFieldsResolver(5,6,7),
                timeCategories,
                false);

        Iterator<Map.Entry<String, SemanticTrajectory>> truthIter = semanticParser.iterator(truthTrajs);
        System.out.println("Doing accuracy measurements for place-matched trajectories.");
        return computeAccuracy(truthIter, semanticTrajectories);
    }

    private static Map<String, SemanticTrajectory> transformIntoSemanticTrajectories(int trajLimit){

        //for geolife use:
        STTrajectoryParser parser = new STTrajectoryParser(
                new ProjectionEquirectangular(),
                new IdFieldResolver(0),
                new LatFieldResolver(1),
                new LonFieldResolver(2),
                new TemporalFieldResolver(6,7),
                true);

        //for synthetic use:
//        STTrajectoryParser parser = new STTrajectoryParser(projection,
//                new IdFieldResolver(0),
//                new NumericFieldsResolver(1,2),
//                new TemporalFieldResolver(3),
//                true);

        Map<String, SpatioCompositeTrajectory<StopEpisode>> stopEpisodeTrajs = new HashMap<>();

        long stopEpisodeClusteringDuration = 0;

        //detect stops and then convert to stop episodes
        Iterator<Map.Entry<String, STTrajectory>> iter = parser.iterator(inputTrajs);

        int nEntriesProcessed = 0;
        int nTrajsProcessed = 0;

        System.out.println("Trajectory, Stop Episodes");

        while(iter.hasNext()){
            Map.Entry<String, STTrajectory> entry = iter.next();
            //System.out.println("Running stop episode clustering on trajectory: " + entry.getKey());

            if(!entry.getValue().isInCartesianMode()){
                entry.getValue().toCartesian();
            }
            double[] stopPrs = algoPosmit.run(entry.getValue(), searchRadius, stopVariance);
            double minStopPr = algoPosmit.estimateMinStopPr(stopPrs);
            STStopTrajectory stopTraj = algoPosmit.toStopTrajectory(entry.getValue(), stopPrs, minStopPr);

            long clusteringStartTime = System.currentTimeMillis();

            SpatioCompositeTrajectory<StopEpisode> stopEpisodes = converter.run(stopTraj, minMoveEpisodeDurationMillis, stopEpisodeDurationMillis, timeCategories);

            stopEpisodeClusteringDuration += (System.currentTimeMillis() - clusteringStartTime);

            stopEpisodeTrajs.put(entry.getKey(), stopEpisodes);

            System.out.println(entry.getKey() + "," + stopEpisodes.size());

            nEntriesProcessed += entry.getValue().size();
            nTrajsProcessed++;
            if(nTrajsProcessed >= trajLimit){
                break;
            }

        }

        output.append(nEntriesProcessed);
        output.append(",");
        System.out.println("Stop episode clustering took (ms): " + stopEpisodeClusteringDuration);
        output.append(stopEpisodeClusteringDuration);
        output.append(",");

        long placeMatchingStartTime = System.currentTimeMillis();

        //make stop episodes with real places using place matching
        Map<String, SemanticTrajectory> estimatedTrajs = semanticBuilder.run(
                stopEpisodeTrajs,
                osmFile,
                matchingBufferMeters,
                matchingAlgo);

        long placeMatchingEndTime = System.currentTimeMillis();
        long placeMatchingDuration = placeMatchingEndTime - placeMatchingStartTime;
        System.out.println("Place-matching took (ms): " + placeMatchingDuration);
        output.append(placeMatchingDuration);
        output.append(",");

        //compute compression
        int nMatchedEntries = estimatedTrajs.values().stream().mapToInt(value -> value.size()).sum();
        double compression  = (nMatchedEntries/(double)nEntriesProcessed) * 100;
        System.out.println("Achieved a compression (%) of: " + compression);
        output.append(compression);
        output.append(",");

        return estimatedTrajs;
    }

    private static double computeAccuracy(Iterator<Map.Entry<String, SemanticTrajectory>> truthIter, Map<String, SemanticTrajectory> estimated){

        double[] accuracies = new double[estimated.size()];

        int i = 0;

        while(truthIter.hasNext()){
            Map.Entry<String, SemanticTrajectory> entry = truthIter.next();
            SemanticTrajectory trueTraj = entry.getValue();
            SemanticTrajectory estimatedTraj = estimated.get(entry.getKey());

            if(estimatedTraj == null){
                continue;
            }

            accuracies[i] += trueTraj.calculateSimilarity(estimatedTraj);

            i++;
        }
        return Maths.mean(accuracies) * 100;
    }


}
