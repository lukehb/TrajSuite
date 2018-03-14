package onethreeseven.trajsuite.experiments.Data;

import onethreeseven.common.util.FileUtil;
import onethreeseven.trajsuite.osm.util.OSMUtil;
import java.io.File;

/**
 * Extract just a smaller sector resolve a large osm file.
 * @author Luke Bermingham
 */
public class ExtractOSMByBounds {

    private static final File bigOSMFile =
            new File(FileUtil.makeAppDir("osm"), "china-latest.osm.pbf");

    private static final File osmExtract =
            new File(FileUtil.makeAppDir("osm_extracts"), "beijing.osm.pbf");

    //Cairns, use: http://boundingbox.klokantech.com/
    private static final double[][] bounds = new double[][]{
            new double[]{115.2631,118.0426}, //longitude
            new double[]{38.8119,40.5368} //latitude
    };

    public static void main(String[] args) {
        OSMUtil.extractSectorFromOSM(bigOSMFile, osmExtract, bounds);
    }


}
