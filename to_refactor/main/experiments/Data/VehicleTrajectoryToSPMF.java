package onethreeseven.trajsuite.experiments;

import com.graphhopper.matching.MatchResult;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.SpatialTrajectoryParser;
import onethreeseven.datastructures.data.resolver.IdFieldResolver;
import onethreeseven.datastructures.data.resolver.LatFieldResolver;
import onethreeseven.datastructures.data.resolver.LonFieldResolver;
import onethreeseven.datastructures.model.SpatialTrajectory;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.spm.data.SPMFWriter;
import onethreeseven.trajsuite.osm.model.GPXFileWrapper;
import onethreeseven.trajsuite.osm.model.GraphHopperDAO;
import onethreeseven.trajsuite.osm.util.MapMatchingUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Get a trajectory data-set, map match each of the trajectories to an underlying road network
 * @author Luke Bermingham
 */
public class VehicleTrajectoryToSPMF {

    private static final File trajFile = new File(FileUtil.makeAppDir("traj"), "buses.txt");
    private static final File osmDataset = new File(FileUtil.makeAppDir("osm_extracts"), "dublin_ireland.osm.pbf");
    private static final Logger logger = Logger.getLogger(VehicleTrajectoryToSPMF.class.getSimpleName());
    private static final File spmfOutFile = new File(FileUtil.makeAppDir("spmf-files"), "buses.txt");
    private static final File refOutFile = new File(FileUtil.makeAppDir("spmf-files"), "buses_id_to_geo.txt");

    public static void main(String[] args) throws IOException {
        //loading
        logger.info("Loading trajectories resolve: " + trajFile.getAbsolutePath());

        //buses
        //1359590401000000,13,0,00131005,2013-01-30,3406,CD,0,-6.273923,53.343307,-235,13003,33608,1998,0
        Map<String, SpatialTrajectory> raw = new SpatialTrajectoryParser(
                new IdFieldResolver(12),
                new LatFieldResolver(9),
                new LonFieldResolver(8),
                new ProjectionEquirectangular(),
                true)
        .setDelimiter(",").parse(trajFile);

        //trucks
        //0862;1;10/09/2002;09:15:59;23.845089;38.018470;486253.80;4207588.10
//        Map<String, SpatialTrajectory> raw = new SpatialTrajectoryParser(new IdFieldResolver(0), 5, 4)
//        .setDelimiter(";").parse(trajFile);

        //tdrive
        //10203,2008-02-02 13:34:26,116.30889,39.99786
//        Map<String, SpatialTrajectory> raw = new SpatialTrajectoryParser(new IdFieldResolver(0), 3, 2)
//                .setDelimiter(",")
//                .parse(trajFile);



        logger.info("Loading OSM file into dao...");
        GraphHopperDAO dao = new GraphHopperDAO(osmDataset);

        Map<Integer, double[]> nodeIdsToLatLon = new HashMap<>();

        //map-matching
        logger.info("Map matching loaded trajectories using osm extract: " + osmDataset.getAbsolutePath());
        int i = 0;
        for (SpatialTrajectory traj : raw.values()) {
            logger.info("Map-matching trail #" + i);

            {
                GPXFileWrapper gpxTrail = new GPXFileWrapper(traj);
                MatchResult mr = dao.mapMatch(gpxTrail, true);
                int[] idNodeSequence = MapMatchingUtil.toSequence(mr);
                //this will keep appending
                new SPMFWriter().write(spmfOutFile, new int[][]{idNodeSequence});
                //merge unique ids into the existing map
                nodeIdsToLatLon.putAll(MapMatchingUtil.getIdToPositionMap(mr, dao));
            }
            i++;
        }

        writeRefMap(nodeIdsToLatLon);
        logger.info("Collect spmf output file at: " + spmfOutFile.getAbsolutePath());
        logger.info("Collect ref output file at: " + refOutFile.getAbsolutePath());
    }

    private static void writeRefMap(Map<Integer, double[]> nodeIdToLatLonMap) throws IOException {

        FileWriter fw = new FileWriter(refOutFile);
        BufferedWriter bw = new BufferedWriter(fw);

        for (Map.Entry<Integer, double[]> entry : nodeIdToLatLonMap.entrySet()) {
            String line = entry.getKey() + "," + entry.getValue()[0] + "," + entry.getValue()[1];
            bw.write(line);
            bw.newLine();
        }

        bw.close();
        fw.close();
    }

}
