package onethreeseven.trajsuite.core.algorithm;


import onethreeseven.datastructures.model.STTrajectory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Uses the average time-step between entries as the uniform interval.
 * @author Luke Bermingham
 */
public class AverageTemporalInterval extends AbstractTemporalIntervalTransform {

    @Override
    long calculateTemporalInterval(Map<String, STTrajectory> trajectories) {

        long totalAvgTime = 0;
        for (STTrajectory stTraj : trajectories.values()) {
            int lastIdx = stTraj.size() - 1;
            LocalDateTime startTime = stTraj.get(0).getTime();
            LocalDateTime endTime = stTraj.get(lastIdx).getTime();
            long totalTrajTime = ChronoUnit.MILLIS.between(startTime, endTime);
            totalAvgTime += (totalTrajTime/lastIdx);
        }
        totalAvgTime /= trajectories.size();
        return totalAvgTime;
    }
}
