package onethreeseven.trajsuite.experiments.Data;

import onethreeseven.common.util.FileUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Merge all plain text files in some directory into one big file.
 * @author Luke Bermingham
 */
public class FileMerger {

    private static final File mergedFile = new File(FileUtil.makeAppDir("traj"), "tdrive.txt");
    private static final File inputDirectory = new File("C:\\Users\\luke\\Downloads\\data-sets\\tdrive\\traces");

    public static void main(String[] args) throws IOException {

        if(!mergedFile.exists() && mergedFile.createNewFile()){

            File[] directoryListing = inputDirectory.listFiles();
            if (directoryListing != null) {
                for (File inputFile : directoryListing) {
                    copyFile(inputFile, mergedFile);
                }
            }
        }
        System.out.println("Get merged file at:" + mergedFile.getAbsolutePath());
    }


    public static void copyFile(File sourceFile, File destFile) throws IOException {
        FileChannel source = new FileInputStream(sourceFile).getChannel();
        FileChannel destination = new FileOutputStream(destFile, true).getChannel();

        long destPos = destination.size();

        destination.transferFrom(source, destPos, source.size());
        source.close();
        destination.close();
    }


}
