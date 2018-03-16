package onethreeseven.trajsuite.experiments.Efficiency;

import com.graphhopper.matching.GPXFile;
import onethreeseven.trajsuite.osm.data.SyntheticVehicleTrajectoryGenerator;
import onethreeseven.trajsuite.osm.model.GPXFileWrapper;
import onethreeseven.trajsuite.osm.model.GraphHopperDAO;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.model.STTrajectory;
import java.io.File;
import java.util.Map;

/**
 * Running time to match synthetic vehicle trajectories to the underlying road network.
 * @author Luke Bermingham
 */
public class VehicleMapMatchingRunningTime {

    private static final File osmFile = new File(FileUtil.makeAppDir("osm_extracts"), "beijing_china.osm.pbf");
    private static final int nTrajs = 100;
    private static final int nEntriesPerTraj = 300000;


    public static void main(String[] args) {

        StringBuilder output = new StringBuilder();
        output.append("NEntries,MapMatching(ms)\n");

        GraphHopperDAO dao = new GraphHopperDAO(osmFile);

        int totalEntries = 0;
        long totalDuration = 0;

        for (int i = 0; i < nTrajs; i++) {

            //generate synthetic trajectory

            System.out.println("Generating trajectory #" + i);
            Map<String, STTrajectory> trajMap =
                    SyntheticVehicleTrajectoryGenerator.generate(osmFile, 1, nEntriesPerTraj);
            System.out.println("Generated trajectory #" + i);
            totalEntries += nEntriesPerTraj;
            GPXFile gpxTraj = new GPXFileWrapper(trajMap.values().iterator().next());
            trajMap.clear();

            //do the actual timing

            long startTime = System.currentTimeMillis();
            dao.mapMatch(gpxTraj, false);
            //List<EdgeMatch> matches = mr.getEdgeMatches();
            long endTime = System.currentTimeMillis();

            totalDuration += (endTime - startTime);

            //append to the output
            output.append(totalEntries);
            output.append(",");
            output.append(totalDuration);
            output.append("\n");

        }

        System.out.println(".");
        System.out.println(".");
        System.out.println(".");
        System.out.println(".");
        System.out.println(output.toString());



    }

}
