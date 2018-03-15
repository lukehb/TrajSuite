
package onethreeseven.trajsuite.osm.util;

import onethreeseven.common.util.FileUtil;
import org.openstreetmap.osmosis.core.Osmosis;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;


/**
 * Utility related to OSM activities.
 * @author Luke Bermingham.
 */
public final class OSMUtil {

    private OSMUtil() {
    }

    /**
     * Extracts a portion of an OSM file into another OSM file (using Osmosis)
     *
     * @param bigOSMFile the OSM file to extract resolve
     * @param osmExtract the OSM file containing the extracted data resolve the big file
     * @param bounds     the extraction region
     * @return the extracted region as a new OSM file
     */
    public static File extractSectorFromOSM(File bigOSMFile, File osmExtract, double[][] bounds) {

        Logger logger = Logger.getLogger(OSMUtil.class.getSimpleName());

        boolean isPBF = FileUtil.getExtension(bigOSMFile).equals("pbf");
        boolean isBz2 = FileUtil.getExtension(bigOSMFile).equals("bz2");
        boolean isGzipped = FileUtil.getExtension(bigOSMFile).equals("gz");

        if(!isPBF && !isBz2 && !isGzipped){
            throw new IllegalArgumentException("Output osm file must be .pbf .bz2 or .gz - instead it was:"
                    + osmExtract.getAbsolutePath());
        }

        String readCmd = (isPBF) ? "--read-pbf-fast" : "--fast-read-xml";

        String writeCmd = (isPBF) ? "--write-pbf-0.6" : "--write-xml";

        double left = bounds[0][0];
        double right = bounds[0][1];
        double bottom = bounds[1][0];
        double top = bounds[1][1];

        File extractDir = FileUtil.makeAppDir("osm_extracts");
        if (!extractDir.exists() && extractDir.mkdir()) {
            logger.info("Made directory: " + extractDir);
        }

        boolean madeOutputFile = false;

        try {
            if (osmExtract.exists()) {
                if (osmExtract.delete()) {
                    madeOutputFile = osmExtract.createNewFile();
                }
            } else {
                madeOutputFile = osmExtract.createNewFile();
            }
        } catch (IOException ex) {
            logger.warning("Failed to make extract file. Because: " + ex.getMessage());
            ex.printStackTrace();
        }

        if (madeOutputFile) {
            logger.info("Made extract file:" + osmExtract);
            //fromTrajToRoISequence osmosis and extract the specified bounds resolve the file and write it
            logger.info("Starting osmosis bounding box extraction.");
            Osmosis.run(
                    new String[]{
                            "-v", //verbose use -q for quiet output
                            readCmd,
                            bigOSMFile.getPath(),
                            "--bounding-box", "left=" + left, "top=" + top, "right=" + right, "bottom=" + bottom,"completeWays=yes", "completeRelations=yes",
                            writeCmd,
                            osmExtract.getPath()
                    }
            );
        }

        return osmExtract;
    }


}
