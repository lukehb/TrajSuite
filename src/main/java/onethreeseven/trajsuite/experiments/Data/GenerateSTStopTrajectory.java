package onethreeseven.trajsuite.experiments.Data;

import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.SpatioCompositeTrajectoryWriter;
import onethreeseven.datastructures.model.STStopTrajectory;
import onethreeseven.datastructures.util.DataGeneratorUtil;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates a stop/move trajectory.
 * @author Luke Bermingham
 */
public class GenerateSTStopTrajectory {

    private static final int nEntries = 100000;
    private static final int nStops = nEntries/20;
    private static final long timeStepMillis = 1000L;
    private static final double maxSpeedMeters = 3;
    private static final double avgNoise = 1;
    private static final double startLat = -16.9186;
    private static final double startLon = 145.7781;
    private static final File outFile = new File(FileUtil.makeAppDir("traj"), "synthetic_100k.txt");

    public static void main(String[] args) {


        STStopTrajectory traj = DataGeneratorUtil.generateTrajectoryWithStops(
                nEntries,
                nStops,
                timeStepMillis,
                timeStepMillis * 20,
                maxSpeedMeters,
                avgNoise,
                startLat,
                startLon);

        traj.toGeographic();

        Map<String, STStopTrajectory> trajs = new HashMap<>();
        trajs.put("0", traj);

        new SpatioCompositeTrajectoryWriter().write(outFile, trajs);

        System.out.println("Collect file at: " + outFile.getAbsolutePath());

    }

}
