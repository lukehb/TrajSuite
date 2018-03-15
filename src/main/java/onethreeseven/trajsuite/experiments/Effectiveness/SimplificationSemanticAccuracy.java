package onethreeseven.trajsuite.experiments.Effectiveness;

import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.STTrajectoryParser;
import onethreeseven.datastructures.data.resolver.*;
import onethreeseven.datastructures.model.STPt;
import onethreeseven.datastructures.model.STTrajectory;
import onethreeseven.datastructures.model.SpatioCompositeTrajectory;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.simplification.algorithm.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * Test how many stops are preserved by a given simplification algorithm.
 * @author Luke Bermingham
 */
public class SimplificationSemanticAccuracy {

    private final static AbstractGeographicProjection projection = new ProjectionEquirectangular();

    private final static File inputTrajs = new File(FileUtil.makeAppDir("traj"), "synthetic_all_40000_noise_1.0.txt");
    private final static File truthTrajs = new File(FileUtil.makeAppDir("traj"), "synthetic_compressed_40000_noise_1.0.txt");

    private final static STTrajectoryParser parser = new STTrajectoryParser(
            projection,
            new IdFieldResolver(0),
            new LatFieldResolver(1),
            new LonFieldResolver(2),
            new TemporalFieldResolver(3),
            true);

    private final static AbstractTrajectorySimplifier[] algos = new AbstractTrajectorySimplifier[]{
            new DRTA(),
            new STDRSED(),
            new DRPD(),
            new SPLPD(),
            new STSPLSED()
    };

    public static void main(String[] args) throws IOException {

        final StringBuilder output = new StringBuilder();

        for (int i = 0; i < algos.length; i++) {
            output.append(algos[i].getClass().getSimpleName());
            output.append(",");
        }
        output.append("\n");

        Map<String, STTrajectory> rawTrajs = parser.parse(inputTrajs);
        Map<String, STTrajectory> expectedTrajs = parser.parse(truthTrajs);
        int totalEntries = rawTrajs.values().stream().mapToInt(value -> value.size()).sum();
        int nStops = expectedTrajs.values().stream().mapToInt(value -> value.size()).sum();

        float simplificationPercentage = 1.0f - (nStops / (float)totalEntries);

        //go through each algorithm and compute the simplification accuracy at preserving stops
        for (AbstractTrajectorySimplifier algo : algos) {
            computeAccuracyForAlgorithm(algo, rawTrajs, expectedTrajs, simplificationPercentage, output);
        }

        //output the results of this experiment
        System.out.println(output.toString());
    }

    private static void computeAccuracyForAlgorithm(AbstractTrajectorySimplifier algo,
                                                    Map<String, STTrajectory> rawTrajs,
                                                    Map<String, STTrajectory> expectedTrajs,
                                                    float simplificationPercentage,
                                                    StringBuilder output){

        double[] accuracies = new double[rawTrajs.size()];

        //get the accuracy of this simplification algorithm at preserving stops
        int i = 0;
        for (Map.Entry<String,STTrajectory> rawEntry : rawTrajs.entrySet()) {
            SpatioCompositeTrajectory<STPt> simplified = algo.simplify(rawEntry.getValue(), simplificationPercentage);
            STTrajectory expected = expectedTrajs.get(rawEntry.getKey());
            double accuracy = calculateAccuracy(simplified, expected);
            accuracies[i] = accuracy;
            i++;
        }

        //get average accuracy
        Arrays.stream(accuracies).average().ifPresent(averageAccuracy -> {
            output.append(averageAccuracy);
            output.append(",");
        });

    }

    public static double calculateAccuracy(SpatioCompositeTrajectory<? extends STPt> actual,
                                                              SpatioCompositeTrajectory<? extends STPt> expected){

        int hits = 0;

        //actual is to larger than expected, or at least as large
        //so iterate expected and then progress through actual until a match is made
        Iterator<? extends STPt> iter = actual.iterator();

        for (STPt expectedEntry : expected) {

            STPt actualEntry;
            while(iter.hasNext()){
                actualEntry = iter.next();
                if(actualEntry.getTime().isEqual(expectedEntry.getTime())){
                    hits++;
                    break;
                }
            }

        }

        return hits / (double)expected.size();

    }

}
