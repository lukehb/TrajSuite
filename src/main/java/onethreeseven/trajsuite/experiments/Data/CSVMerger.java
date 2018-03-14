package onethreeseven.trajsuite.experiments.Data;


import onethreeseven.common.util.FileUtil;

import java.io.*;

/**
 * Merge all csv files in a directory together.
 * @author Luke Bermingham
 */
public class CSVMerger {

    private static final File csvDirectory = new File("C:\\Users\\luke\\AppData\\Local\\Temp\\geolife_merged\\beijing");

    private static final File outFile = new File(FileUtil.makeAppDir("traj"), "geolife_179.txt");

    private static final String delimiter = ",";

    private static final boolean includeFilename = true;

    public static void main(String[] args) throws IOException {

        BufferedWriter bw;
        if (outFile.createNewFile() && outFile.canWrite()) {
            bw = new BufferedWriter(new FileWriter(outFile));
            mergeFiles(bw);
        }
        else{
            System.out.println("File already exists at: " + outFile.getAbsolutePath());
        }
    }

    private static void mergeFiles(BufferedWriter bw) throws IOException {
        //go through each file in the directory
        File[] directoryListing = csvDirectory.listFiles();
        if (directoryListing != null) {
            int i = 0;
            for (File csvFile : directoryListing) {

                BufferedReader br = new BufferedReader(new FileReader(csvFile));
                //read over the first line
                if(br.ready()){
                    br.readLine();
                }

                String fileName = csvFile.getName().split("\\.")[0];
                final String id = (includeFilename) ? fileName : String.valueOf(i);

                String line;
                while( (line = br.readLine()) != null ){
                    write(bw, id + delimiter + line);
                }
                i++;
                br.close();

            }
        }
        bw.close();
    }

    public static void write(BufferedWriter writer, String toWrite) throws IOException {
        writer.write(toWrite);
        writer.newLine();
    }


}
