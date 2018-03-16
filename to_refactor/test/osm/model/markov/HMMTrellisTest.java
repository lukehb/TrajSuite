package onethreeseven.trajsuite.osm.model.markov;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Testing the extraction of the Viterbi path using the wikipedia example:
 * https://en.wikipedia.org/wiki/Viterbi_algorithm
 * @author Luke Bermingham
 */
public class HMMTrellisTest {

    private static int[] states = {0, 1};
    private static int[] observations = {0, 1, 2};
    private static double[] startProbability = {0.6, 0.4};
    private static double[][] transitionProbability = {{0.7, 0.3}, {0.4, 0.6}};
    private static double[][] emissionProbability = {{0.5, 0.4, 0.1}, {0.1, 0.3, 0.6}};

    private static Map<Integer, String> stateToText = new HashMap<>();

    static {
        stateToText.put(0, "Healthy");
        stateToText.put(1, "Fever");
    }


    private class MockState extends MarkovState {
        private final int observationIdx;
        private final int id;

        MockState(int id, int observationIdx) {
            this.id = id;
            this.observationIdx = observationIdx;
        }

        int getId(){
            return id;
        }

        @Override
        public double getLogEmissionPr() {
            return emissionProbability[getId()][observationIdx];
        }

        @Override
        public double getLogTransitionPr(MarkovState toState) {
            return transitionProbability[getId()][((MockState)toState).getId()];
        }
    }


    @Test
    public void testGetViterbiPath() throws Exception {

        //in this example the state ids are the indexes into the arrays
        final MockState rootState = new MockState(-1, -1) {
            @Override
            public double getLogEmissionPr() {
                return 0;
            }

            @Override
            public double getLogTransitionPr(MarkovState toState) {
                return startProbability[((MockState)toState).getId()];
            }
        };

        MockState[] firstStates = new MockState[states.length];
        for (int i = 0; i < states.length; i++) {
            firstStates[i] = new MockState(i, 0);
        }

        HMMTrellis tree = new HMMTrellis(rootState, firstStates, false, 100);

        //do insertions
        for (int i = 1; i < observations.length; i++) {
            for (int j = 0; j < states.length; j++) {
                tree.insert(new MockState(j, i));
            }
            tree.moveToNextState();
        }

        List<MarkovState> path = tree.getViterbiPath();

        Assert.assertTrue(path.size() == observations.length);
        Assert.assertTrue(stateToText.get(((MockState)path.get(0)).getId()).equals("Healthy"));
        Assert.assertTrue(stateToText.get(((MockState)path.get(1)).getId()).equals("Healthy"));
        Assert.assertTrue(stateToText.get(((MockState)path.get(2)).getId()).equals("Fever"));

    }




}