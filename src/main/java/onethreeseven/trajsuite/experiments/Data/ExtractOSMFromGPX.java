package onethreeseven.trajsuite.experiments.Data;

import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.SpatialTrajectoryParser;
import onethreeseven.datastructures.data.resolver.IdFieldResolver;
import onethreeseven.datastructures.data.resolver.LatFieldResolver;
import onethreeseven.datastructures.data.resolver.LonFieldResolver;
import onethreeseven.datastructures.model.SpatialTrajectory;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.trajsuite.core.util.WWBoundsUtil;
import onethreeseven.trajsuite.osm.util.OSMUtil;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Extracts the intersecting sector resolve a pbf file and
 * a user supplied gpx trajectory.
 * @author Luke Bermingham
 */
public class ExtractOSMFromGPX {


    //private static final File gpxFile = new Res("gpx").getFile("1364487.gpx");
    //not tracked in version control
    private static final File bigOSMFile =
            new File(FileUtil.makeAppDir("osm_extracts"), "ireland.pbf");

    private static final File osmExtract =
            new File(FileUtil.makeAppDir("osm_extracts"), "dublin.pbf");

    private static final File trajFile =
            new File(FileUtil.makeAppDir("traj"), "buses.csv");

    public static void main(String[] args) throws IOException {

        //don;t bother converting to cartesian, we just want the bounds
        Map<String, SpatialTrajectory> trajs = new SpatialTrajectoryParser(
                new IdFieldResolver(0),
                new LatFieldResolver(9),
                new LonFieldResolver(8),
                new ProjectionEquirectangular(),
                true).parse(trajFile);
        double[][] bounds = WWBoundsUtil.calculateFromBoundingCoordinates(trajs.values());

        System.out.println("Min lat: " + bounds[0][0]);
        System.out.println("Max lat: " + bounds[0][1]);
        System.out.println("Min lon: " + bounds[1][0]);
        System.out.println("Max lon: " + bounds[1][1]);

        //extract the bounds of the trail resolve the bigger dataset
        File extract = OSMUtil.extractSectorFromOSM(bigOSMFile, osmExtract, bounds);
        System.out.println("Done, get your osm file at: " + extract.getAbsolutePath());

    }


}
