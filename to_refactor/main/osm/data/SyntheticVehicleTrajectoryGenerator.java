package onethreeseven.trajsuite.osm.data;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.AStar;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint3D;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.model.STTrajectory;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Make a synthetic vehicle trajectory using graph hopped and an OSM file.
 * @author Luke Bermingham
 */
public class SyntheticVehicleTrajectoryGenerator {

    /**
     * Generates a trajectory that follows a road network.
     * It is quite dumb. It just moves at 10s increments along road edges and doesn't ever stop.
     * @param osmFile The osm file to use for the road networks.
     * @param nTrajs The number of trajectories to make.
     * @param nEntriesPerTraj The number of entries to make per trajectory.
     * @return The vehicle trajectories.
     */
    public static Map<String, STTrajectory> generate(File osmFile, int nTrajs, int nEntriesPerTraj){

        final FlagEncoder flagEncoder = new CarFlagEncoder();
        final GraphHopper gh = setupGH(osmFile, flagEncoder);
        final Map<String, STTrajectory> output = new HashMap<>();

        for (int i = 0; i < nTrajs; i++) {

            //make a synthetic path between two points in the OSM file
            PointList pts = null;
            while(pts == null || pts.size() <= 1){
                Path route = generateRandomRoute(gh, flagEncoder);
                pts = route.calcPoints();
            }
            output.put(String.valueOf(i), makeTraj(pts, nEntriesPerTraj));
        }

        return output;
    }

    private static Path generateRandomRoute(GraphHopper gh, FlagEncoder flagEncoder){

        Random rand = new Random();
        int nNodes = gh.getGraphHopperStorage().getNodes();
        int startNode = rand.nextInt(nNodes);
        int endNode = rand.nextInt(nNodes);

        //route between them using gh

        AStar algo = new AStar(gh.getGraphHopperStorage(),
                new FastestWeighting(flagEncoder),
                TraversalMode.NODE_BASED);
        return algo.calcPath(startNode, endNode);
    }

    private static STTrajectory makeTraj(PointList pts, int nEntriesPerTraj){

        final int nEntriesPerEdge = nEntriesPerTraj / (pts.size() - 1);
        LocalDateTime curTime = LocalDateTime.now();

        STTrajectory output = new STTrajectory(false, new ProjectionEquirectangular());

        Iterator<GHPoint3D> iter = pts.iterator();
        GHPoint3D prev = iter.next();
        while(iter.hasNext()){
            GHPoint3D cur = iter.next();

            double latOffset = (cur.lat - prev.lat)/(nEntriesPerEdge-1);
            double lonOffset = (cur.lon - prev.lon)/(nEntriesPerEdge-1);

            for (int i = 0; i < nEntriesPerEdge; i++) {
                double lat = prev.lat + (latOffset * i);
                double lon = prev.lon + (lonOffset * i);
                curTime = curTime.plusSeconds(10);
                output.addGeographic(new double[]{lat, lon}, curTime);
            }

            prev = cur;
        }
        return output;
    }

    private static GraphHopper setupGH(File osmFile, FlagEncoder flagEncoder){
        GraphHopper gh = new GraphHopperOSM().setOSMFile(osmFile.getAbsolutePath());
        File workingDir = FileUtil.makeAppDir("graphHopper_" + osmFile.getName());
        gh.setGraphHopperLocation(workingDir.getAbsolutePath());
        gh.setEncodingManager(new EncodingManager(flagEncoder));
        gh.setCHEnabled(true);
        gh.importOrLoad();
        return gh;
    }

}
