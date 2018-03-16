package onethreeseven.trajsuite.experiments.Data;

import onethreeseven.common.model.TimeCategoryPool;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.STTrajectoryParser;
import onethreeseven.datastructures.data.SpatioCompositeTrajectoryWriter;
import onethreeseven.datastructures.data.resolver.*;
import onethreeseven.datastructures.model.STStopTrajectory;
import onethreeseven.datastructures.model.STTrajectory;
import onethreeseven.datastructures.model.SpatioCompositeTrajectory;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.geo.projection.ProjectionMercator;
import onethreeseven.stopmove.algorithm.POSMIT;
import onethreeseven.trajsuite.osm.algorithm.SeClust;
import onethreeseven.trajsuite.osm.algorithm.SemanticTrajectoryBuilder;
import onethreeseven.trajsuite.osm.model.SemanticTrajectory;
import onethreeseven.trajsuite.osm.model.StopEpisode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Convert an OSM bz2 file to semantic trace
 * @author Luke Bermingham
 */
public class MakeSemanticTrajectories {

    ////////////////
    //Input params
    ////////////////
    private static final File osmFile = new File(FileUtil.makeAppDir("osm_extracts"), "beijing_china.osm.pbf");
    private static final File trajInFile = new File(FileUtil.makeAppDir("traj"), "geolife_179.txt");
    private static final File outFile = new File(FileUtil.makeAppDir("semantic"), "geolife_179_semantic.txt");
    //////////////////////////
    //Stop conversion params
    /////////////////////////
    private static final long mergeNearbyStopMillis = 10000L; //merge any stop entries within 10s
    private static final long minStopDurationMillis = 1000L * 120; //2 minutes
    private static final int posmitSearchWidth = 7;
    private static final double posmitMinStopProbability = 0.8;
    private static final TimeCategoryPool timeCategoryPool = TimeCategoryPool.TIMES_OF_DAY;
    /////////////////////////////
    //Place matching parameters
    /////////////////////////////
    private static final int placeMatchingBufferMeters = 200;
    private static final boolean useWebQueries = false;


    public static void main(String[] args) throws IOException {
        Map<String, SemanticTrajectory> out = new SemanticTrajectoryBuilder()
                .setUseWebQueries(useWebQueries)
                .run(createStopTrajs(), osmFile, placeMatchingBufferMeters);

        new SpatioCompositeTrajectoryWriter().write(outFile, out);
        System.out.println("Get semantic trajectory output at: " + outFile.getAbsolutePath());
    }

    private static Map<String, SpatioCompositeTrajectory<StopEpisode>> createStopTrajs() throws IOException {

        System.out.println("Converting trajectories to stop episodes...");

        //merged dataset
        STTrajectoryParser parser = new STTrajectoryParser(
                new ProjectionEquirectangular(),
                new IdFieldResolver(0),
                new LatFieldResolver(1),
                new LonFieldResolver(2),
                new TemporalFieldResolver(6,7),
                true);

        //individual geolife trajectories
//        STTrajectoryParser parser = new STTrajectoryParser(
//                new ProjectionMercator(),
//                new SameIdResolver("0"),
//                new NumericFieldsResolver(0,1),
//                new TemporalFieldResolver(5,6),
//                true);

        final Map<String, STTrajectory> trajs = parser.parse(trajInFile);
        final POSMIT posmit = new POSMIT();
        final HashMap<String, SpatioCompositeTrajectory<StopEpisode>> out = new HashMap<>();
        final SeClust converter = new SeClust();

        for (Map.Entry<String, STTrajectory> entry : trajs.entrySet()) {
            if(entry.getValue().size() <= 3){
                continue;
            }

            double estimatedStopVariance = posmit.estimateStopVariance(entry.getValue());
            double[] stopProbabilities = posmit.run(entry.getValue(), posmitSearchWidth, estimatedStopVariance);
            STStopTrajectory stopTraj = posmit.toStopTrajectory(entry.getValue(), stopProbabilities, posmitMinStopProbability);
            out.put(entry.getKey(), converter.run(stopTraj, mergeNearbyStopMillis, minStopDurationMillis, timeCategoryPool));
        }
        return out;
    }




}
