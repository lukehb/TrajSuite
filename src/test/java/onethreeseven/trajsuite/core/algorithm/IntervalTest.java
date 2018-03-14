package onethreeseven.trajsuite.core.algorithm;

import onethreeseven.datastructures.model.STPt;
import onethreeseven.datastructures.model.STTrajectory;
import onethreeseven.datastructures.util.DataGeneratorUtil;
import org.junit.Assert;
import org.junit.Test;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Testing interval data transformation on some generated trajectory data.
 * @see MinTemporalInterval
 * @see MaxTemporalInterval
 * @see AverageTemporalInterval
 * @see DefinedTemporalInterval
 * @see TimeUnitTemporalInterval
 * @author Luke Bermingham.
 */
public class IntervalTest {

    private static final double[][] bounds = new double[][]{
            new double[]{-180, 180},
            new double[]{-90, 90}
    };

    private static final int nTrajs = 10;
    private static final boolean inCartesianMode = false;
    private static final long startTime = System.currentTimeMillis();
    private static final Random rand = new Random();


    public void checkSameInterval(Map<String, STTrajectory> trajectories){

        long expectedInterval;
        {
            STTrajectory traj = trajectories.values().iterator().next();
            Iterator<STPt> stIter = traj.iterator();
            expectedInterval = ChronoUnit.MILLIS.between(stIter.next().getTime(), stIter.next().getTime());
        }

        for (STTrajectory trajectory : trajectories.values()) {
            LocalDateTime prev = trajectory.get(0).getTime();
            for (int i = 1; i < trajectory.size(); i++) {
                LocalDateTime cur = trajectory.get(i).getTime();
                long curInterval = ChronoUnit.MILLIS.between(prev, cur);
                //check that the interval is what we expect
                Assert.assertEquals(expectedInterval, curInterval);
                prev = cur;
            }
        }
    }

    public void generateNormaliseAndTest(AbstractTemporalIntervalTransform temporalIntervalTransform){
        Map<String, STTrajectory> rawTrajs = DataGeneratorUtil.generateSpatiotemporalTrajectories(
                bounds, startTime, nTrajs, inCartesianMode, ()-> rand.nextInt(3000));
        Map<String, STTrajectory> normalisedTrajs = temporalIntervalTransform.run(rawTrajs);

        //go through trajectories and make sure the interval is the same for all of them
        checkSameInterval(normalisedTrajs);
    }

    @Test
    public void testMaxInterval() throws Exception {
        generateNormaliseAndTest(new MaxTemporalInterval());
    }

    @Test
    public void testMinInterval() throws Exception {
        generateNormaliseAndTest(new MinTemporalInterval());
    }


    @Test
    public void testAvgInterval() throws Exception {
        generateNormaliseAndTest(new AverageTemporalInterval());
    }

    @Test
    public void testDefinedInterval() throws Exception {
        generateNormaliseAndTest(new DefinedTemporalInterval(500));
    }

    @Test
    public void testTemporalInterval() throws Exception {
        //wrap it into using seconds for the interval
        TimeUnitTemporalInterval definedIntervalAlgo =
                TimeUnitTemporalInterval.wrap(new DefinedTemporalInterval(1200), TimeUnit.SECONDS);
        generateNormaliseAndTest(definedIntervalAlgo);
    }


}