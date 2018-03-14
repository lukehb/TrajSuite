package onethreeseven.trajsuite.experiments.Data;

import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.STTrajectoryParser;
import onethreeseven.datastructures.data.resolver.*;
import onethreeseven.datastructures.model.STTrajectory;
import onethreeseven.datastructures.model.Trajectory;
import onethreeseven.datastructures.util.TrajectoryStatistician;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.spm.data.SPMFParser;
import java.io.File;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Calculates stats about a trajectory data-set.
 * @author Luke Bermingham
 */
public class CalculateTrajectoryStats {

    private static final boolean isSPMF = true;
    private static final File trajInFile = new File(FileUtil.makeAppDir("spmf"), "geolife_179_semantic_homes_PLACE_NAME.txt");

    public static void main(String[] args) throws IOException {

        System.out.println("Trajectory stats for: " + trajInFile.getName());

        if(isSPMF){
            Collection<Trajectory> trajs = spmfToTrajs(trajInFile);
            TrajectoryStatistician.printSizeStats(trajs);
        }
        else{
            STTrajectoryParser parser = new STTrajectoryParser(
                    new ProjectionEquirectangular(),
                    new IdFieldResolver(0),
                    new LatFieldResolver(1),
                    new LonFieldResolver(2),
                    new TemporalFieldResolver(6,7),
                    true);

            Map<String, STTrajectory> trajs = parser.parse(trajInFile);

            TrajectoryStatistician.printSizeStats(trajs.values());
            TrajectoryStatistician.printTemporalStats(trajs.values(), ChronoUnit.DAYS);
        }
    }

    private static Collection<Trajectory> spmfToTrajs(File spmfFile){

        SPMFParser parser = new SPMFParser();
        int[][] seqs = parser.parseSequences(spmfFile);

        Collection<Trajectory> out = new ArrayList<>();

        for (int[] seq : seqs) {
            Trajectory traj = new Trajectory();
            for (int item : seq) {
                traj.add(new double[]{item});
            }
            out.add(traj);
        }
        return out;
    }

}
