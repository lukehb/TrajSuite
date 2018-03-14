package onethreeseven.trajsuite.experiments.Data;

import javafx.application.Application;
import javafx.stage.Stage;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.STTrajectoryParser;
import onethreeseven.datastructures.data.resolver.IdFieldResolver;
import onethreeseven.datastructures.data.resolver.LatFieldResolver;
import onethreeseven.datastructures.data.resolver.LonFieldResolver;
import onethreeseven.datastructures.data.resolver.TemporalFieldResolver;
import onethreeseven.datastructures.model.STTrajectory;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.roi.algorithm.HybridRoIs;
import onethreeseven.roi.algorithm.TrajectoryRoIUtil;
import onethreeseven.roi.model.MiningSpaceFactory;
import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIGrid;
import onethreeseven.spm.data.SPMFWriter;
import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * Load trajectory, find RoIs, find visitations, write visitation.
 * @author Luke Bermingham
 */
public class TrajectoryToRoIVisitations extends Application {

    private final File trajFile =
            new File(FileUtil.makeAppDir("trajectories/tdrive"), "366.txt");

    private final File outFile =
            new File(FileUtil.makeAppDir("spmf"), "roiVisits.spmf");

    @Override
    public void start(Stage primaryStage) throws Exception {

        //load trajectory
        STTrajectoryParser parser = new STTrajectoryParser(
                new ProjectionEquirectangular(),
                new IdFieldResolver(0),
                new LatFieldResolver(5),
                new LonFieldResolver(6),
                new TemporalFieldResolver(1),
                true);
        Map<String, STTrajectory> trajectories = parser.parse(trajFile);

        //find rois
        System.out.println("Finding RoIs");
        RoIGrid roIGrid = MiningSpaceFactory.createGrid(trajectories, new int[]{100, 10, 1}, 0);
        Collection<RoI> rois = new HybridRoIs().run(roIGrid, 4);

        //find visitations
        int[][] sequences = new int[trajectories.size()][];
        int i = 0;
        for (STTrajectory trajectory : trajectories.values()) {
            i++;
            System.out.println("Finding visitations");
            sequences[i] = TrajectoryRoIUtil.fromTrajToRoISequence(trajectory, rois, roIGrid);
            i++;
        }

        //write visitations
        System.out.println("Writing Visitations");
        new SPMFWriter().write(outFile, sequences);
        System.out.println("Get output file at: " + outFile.getAbsolutePath());

    }
}
