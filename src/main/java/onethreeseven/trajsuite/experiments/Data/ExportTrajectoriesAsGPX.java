package onethreeseven.trajsuite.experiments.Data;

import com.graphhopper.matching.GPXFile;
import onethreeseven.datastructures.data.resolver.IdFieldResolver;
import onethreeseven.datastructures.data.resolver.LatFieldResolver;
import onethreeseven.datastructures.data.resolver.LonFieldResolver;
import onethreeseven.datastructures.data.resolver.TemporalFieldResolver;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.trajsuite.osm.model.GPXFileWrapper;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.STTrajectoryParser;
import onethreeseven.datastructures.model.STTrajectory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Loads trajectories and writes them as GPX files
 * @author Luke Bermingham
 */
public class ExportTrajectoriesAsGPX {

    private static final File trajFile = new File(FileUtil.makeAppDir("trajectories/tdrive"), "366.txt");

    private static final File outDir =
            FileUtil.makeAppDir("gpxOut");

    public static void main(String[] args) throws IOException {

        if(!outDir.exists() && outDir.mkdir()){
            System.out.println("Created dir: " + outDir.getAbsolutePath());
        }

        Map<String, STTrajectory> trajs = new STTrajectoryParser(
                new ProjectionEquirectangular(),
                new IdFieldResolver(0),
                new LatFieldResolver(3),
                new LonFieldResolver(2),
                new TemporalFieldResolver(1),
                true).parse(trajFile);

        GPXFile[] gpxFiles = GPXFileWrapper.fromTrajectories(trajs);

        String filePrefix = trajFile.getName().substring(0, trajFile.getName().lastIndexOf('.'));

        int i = 0;
        for (GPXFile gpxFile : gpxFiles) {
            File outFile = new File(outDir, filePrefix + i + ".gpx");
            if(!outFile.exists() && outFile.createNewFile()){
                System.out.println("Created file: " + outFile.getAbsolutePath());
            }
            gpxFile.doExport(outFile.getAbsolutePath());
            System.out.println("Writing gpx to: " + outFile.getAbsolutePath());
            i++;
        }

    }

}
