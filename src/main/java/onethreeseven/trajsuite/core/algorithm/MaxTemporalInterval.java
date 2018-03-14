package onethreeseven.trajsuite.core.algorithm;

import onethreeseven.datastructures.model.STPt;
import onethreeseven.datastructures.model.STTrajectory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Make all entries in a certain dimensions of a trajectory have
 * the maximum interval found in the raw trajectory. For example if the
 * the biggest gap in time between two entries in the original data-set
 * was 10 minutes, after the algorithm ran all entries would have a 10 minutes interval.
 * @author Luke Bermingham.
 */
public class MaxTemporalInterval extends AbstractTemporalIntervalTransform {

    @Override
    protected long calculateTemporalInterval(Map<String, STTrajectory> trajectories) {
        long maxInterval = Long.MIN_VALUE;

        for (Map.Entry<String, STTrajectory> trajEntry : trajectories.entrySet()) {
            STTrajectory trajectory = trajEntry.getValue();
            //previous value is used to determine interval size
            LocalDateTime prevTime = trajectory.get(0).getTime();
            //go through each point in trajectory
            for (STPt stPt : trajectory) {
                LocalDateTime curTime = stPt.getTime();
                long temporalInterval = ChronoUnit.MILLIS.between(prevTime, curTime);
                if (temporalInterval > maxInterval) {
                    maxInterval = temporalInterval;
                }
                prevTime = curTime;
            }
        }

        return maxInterval;
    }

}
