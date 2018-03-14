package onethreeseven.trajsuite.experiments.Data;

import onethreeseven.common.model.TimeCategoryPool;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.SpatioCompositeTrajectoryWriter;
import onethreeseven.datastructures.data.resolver.IdFieldResolver;
import onethreeseven.datastructures.data.resolver.NumericFieldsResolver;
import onethreeseven.datastructures.data.resolver.TemporalFieldResolver;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.trajsuite.osm.data.SemanticPlaceFieldsResolver;
import onethreeseven.trajsuite.osm.data.SemanticTrajectoryParser;
import onethreeseven.trajsuite.osm.data.SyntheticSemanticTrajectoryGenerator;
import onethreeseven.trajsuite.osm.model.SemanticTrajectory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * Creates synthetic semantic trajectories.
 * @author Luke Bermingham
 */
public class MakeSyntheticSemanticTrajectories {

    private static final File osmFile = new File(FileUtil.makeAppDir("osm_extracts"), "beijing_china.osm.pbf");
    private static final int nPlaces = 100;
    private static final int nTrajs = 100;
    private static final int nEpisodes = 10;
    private static final int nEntriesPerEpisode = 20;
    private static final double spatialNoise = 10;
    private static final long episodeDurationMillis = 100000L * nEntriesPerEpisode;
    private static final int durationMultiplier = 2;
    private static final AbstractGeographicProjection projection = new ProjectionEquirectangular();

    private static final boolean generateSyntheticTrajectories = true;
    private static final boolean compressStopMoveEntries = true;

    public static void main(String[] args) {

        int totalEntries = nTrajs * (nEpisodes * 2) * nEntriesPerEpisode;
        System.out.println("This many entries: " + totalEntries);

        String outputST = "synthetic_all_" + totalEntries + "_noise_" + spatialNoise + ".txt";
        File outFileST = new File(FileUtil.makeAppDir("traj"), outputST);

        //output the spatio-temporal trajectories
        if(generateSyntheticTrajectories){
            SyntheticSemanticTrajectoryGenerator gen = new SyntheticSemanticTrajectoryGenerator();
            gen.makeTrajectories(projection,
                    osmFile,
                    nPlaces,
                    nTrajs,
                    nEpisodes,
                    nEntriesPerEpisode,
                    spatialNoise,
                    LocalDateTime.now(),
                    episodeDurationMillis,
                    durationMultiplier,
                    TimeCategoryPool.TIMES_OF_DAY,
                    outFileST);

            System.out.println("Done making semantic trajectories.");
        }

        if(compressStopMoveEntries){
            System.out.println("Now compressing raw stop-move entries into single stop locations.");
            compressStopMoveEntries(totalEntries, outFileST);
        }

        System.out.println("Done");
    }

    private static void compressStopMoveEntries(int totalEntries, File semanticTrajFile){

        //output the semantic trajectory with duplicated removed
        String outputName = "synthetic_compressed_" + totalEntries + "_noise_" + spatialNoise + ".txt";
        File outFileSynth = new File(FileUtil.makeAppDir("traj"), outputName);
        //go through and remove redundant stop locations

        SemanticTrajectoryParser parser = new SemanticTrajectoryParser(projection,
                new IdFieldResolver(0),
                new NumericFieldsResolver(1,2),
                new TemporalFieldResolver(3),
                new SemanticPlaceFieldsResolver(5,6,7),
                TimeCategoryPool.TIMES_OF_DAY,
                false);

        Iterator<Map.Entry<String, SemanticTrajectory>> datasetIter = parser.iterator(semanticTrajFile);

        final SpatioCompositeTrajectoryWriter writer = new SpatioCompositeTrajectoryWriter();

        System.out.println("Reading trajectories one by one from: " + semanticTrajFile.getAbsolutePath());

        while(datasetIter.hasNext()){

            Map.Entry<String, SemanticTrajectory> entry = datasetIter.next();
            SemanticTrajectory trajectory = entry.getValue();

            System.out.println("Compressing trajectory: " + entry.getKey());

            SyntheticSemanticTrajectoryGenerator.compressSemanticTrajectory(trajectory);

            System.out.println("Writing trajectory: " + entry.getKey() + " to: " + outFileSynth.getAbsolutePath());

            writer.write(outFileSynth, Collections.singletonMap(entry.getKey(), trajectory));
        }
    }

}
