package onethreeseven.trajsuite.experiments.Data;


import onethreeseven.common.util.FileUtil;
import onethreeseven.geo.model.LatLonBounds;
import java.io.*;
import java.util.logging.Logger;

/**
 * Point it at geo-life directory and it will merge the files
 * and separate files that are outside Beijing
 * @author Luke Bermingham
 */
public class GeolifeMerger {

    private static final int linesToSkip = 6;
    private static final Logger log = Logger.getLogger(GeolifeMerger.class.getSimpleName());

    private static final File geolifeDir =
            new File("C:\\Users\\luke\\Downloads\\data-sets\\Geolife Trajectories 1.3\\Data");

    private static final File beijingDir = FileUtil.makeAppDir("geolife_merged/beijing");
    private static final File notBeijingDir = FileUtil.makeAppDir("geolife_merged/not_beijing");

    private static final String subDir = "Trajectory";

    public static void main(String[] args) throws IOException {

        File[] trajDirs = geolifeDir.listFiles();
        if(trajDirs == null){
            log.warning("There was no directories in:  " + geolifeDir.getAbsolutePath());
            return;
        }
        //go through each directory and merge the files in their
        for (File trajDir : trajDirs) {
            log.info("Merging trajectories in directory: " + trajDir.getName());
            mergeFiles(trajDir);
        }
        log.info("Done");
    }

    private static void mergeFiles(File trajDir) throws IOException {

        File outFile = new File(beijingDir, trajDir.getName() + ".txt");
        if(!outFile.exists()){
            if(!outFile.createNewFile()){
                log.severe("Could not createAnnotation file: " + outFile.getName());
                return;
            }
        }

        File[] trajSegments = new File(trajDir, subDir).listFiles();

        if(trajSegments == null){
            log.warning("There was no directories in:  " + geolifeDir.getAbsolutePath());
            return;
        }

        //boolean isInBeijing = false;
        FileWriter fw = new FileWriter(outFile);
        BufferedWriter bw = new BufferedWriter(fw);

        try{
            //go through each directory and merge the files in their
            for (File traj : trajSegments) {
                FileReader fr = new FileReader(traj);
                BufferedReader br = new BufferedReader(fr);
                try{
                    String line;

                    //skip some lines
                    for (int i = 0; i < linesToSkip; i++) {
                        br.readLine();
                    }

                    while( (line = br.readLine()) != null ){
                        if(isInBeijing(line)){
                            bw.write(line);
                            bw.newLine();
                        }
                    }
                }catch (Exception e){
                    log.severe(e.getMessage());
                }
                br.close();
                fr.close();
            }
        }catch (Exception e){
            log.severe(e.getMessage());
        }

        bw.close();
        fw.close();

//        if(!isInBeijing){
//            File targetFile = new File(notBeijingDir, trajDir.getName() + ".txt");
//            Files.move(outFile.toPath(), targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
//            log.info("Moved merged file to: " + targetFile.getAbsolutePath());
//        }

        log.info("Merged traj to: " + outFile.getAbsolutePath());

    }

    private static final LatLonBounds beijingBB =
            new LatLonBounds(38.8119,40.5368,115.2631,118.0426);

    private static boolean isInBeijing(String line){
        if(line == null || line.isEmpty()){return false;}
        String[] parts = line.split(",");
        double lat = Double.parseDouble(parts[0]);
        double lon = Double.parseDouble(parts[1]);
        return beijingBB.contains(lat, lon);
    }

}
