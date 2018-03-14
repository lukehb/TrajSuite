package onethreeseven.trajsuite.core.algorithm;

import onethreeseven.datastructures.model.STTrajectory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Make all entries in a certain dimensions of a trajectory have
 * the minimum interval found in the raw trajectory. For example if the
 * the smallest gap in time between two entries in the original data-set
 * was 2 minutes, after the algorithm ran all entries would have a 2 minutes interval.
 * @author Luke Bermingham.
 */
public class MinTemporalInterval extends AbstractTemporalIntervalTransform {

    @Override
    long calculateTemporalInterval(Map<String, STTrajectory> trajectories) {
        long minInterval = Long.MAX_VALUE;

        for (STTrajectory trajectory : trajectories.values()) {
            //previous value is used to determine interval size
            LocalDateTime prevValue = trajectory.get(0).getTime();
            //go through each point in trajectory
            for (int i = 1; i < trajectory.size(); i++) {
                LocalDateTime curValue = trajectory.get(i).getTime();
                long interval = ChronoUnit.MILLIS.between(prevValue, curValue);
                if (interval != 0 && interval < minInterval) {
                    minInterval = interval;
                }
                prevValue = curValue;
            }
        }

        return minInterval;
    }
}
