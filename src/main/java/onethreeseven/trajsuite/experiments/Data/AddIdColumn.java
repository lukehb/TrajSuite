package onethreeseven.trajsuite.experiments.Data;

import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.STStopTrajectoryParser;
import onethreeseven.datastructures.data.SpatioCompositeTrajectoryWriter;
import onethreeseven.datastructures.data.resolver.*;
import onethreeseven.datastructures.model.STStopTrajectory;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Quick little util to add a column to st-stop trajectory files
 * @author Luke Bermingham
 */
public class AddIdColumn {

    private static final String filename = "short_walk";

    private static final File trajFile = new File(FileUtil.makeAppDir("traj"), filename + ".txt");
    private static final File outFile = new File(FileUtil.makeAppDir("col"), filename + ".txt");
    private static final AbstractGeographicProjection projection = new ProjectionEquirectangular();

    public static void main(String[] args) throws IOException {

        Map<String, STStopTrajectory> trajMap = new STStopTrajectoryParser(
                projection,
                new SameIdResolver("1"),
                new LatFieldResolver(0),
                new LonFieldResolver(1),
                new TemporalFieldResolver(2),
                new StopFieldResolver(3),
                true).parse(trajFile);

        new SpatioCompositeTrajectoryWriter().write(outFile, trajMap);
        System.out.println("Wrote file to: " + outFile.getAbsolutePath());

    }

}
