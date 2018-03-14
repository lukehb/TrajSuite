package onethreeseven.trajsuite.core.algorithm;

import onethreeseven.common.util.Maths;
import onethreeseven.datastructures.model.STPt;
import onethreeseven.datastructures.model.STTrajectory;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Algorithms that extend this class modify the trajectory so that each entry has exactly the same
 * number of milliseconds between it.
 * {@link STTrajectory}.
 * @author Luke Bermingham.
 */
abstract class AbstractTemporalIntervalTransform {

    /**
     * Get the interval to apply to the trajectories using some heuristic
     *
     * @param trajectories the trajectories to  apply regular interval to
     * @return the interval to apply
     */
    abstract long calculateTemporalInterval(Map<String, STTrajectory> trajectories);

    public Map<String, STTrajectory> run(Map<String, STTrajectory> stTrajs) {

        //find the temporal interval
        long interval = calculateTemporalInterval(stTrajs);

        //we make a copy because this retains the index to field mappings
        Map<String, STTrajectory> result = new HashMap<>();

        //createAnnotation uniform interval trajectory
        for (Map.Entry<String, STTrajectory> entry : stTrajs.entrySet()) {
            result.put(entry.getKey(), createUniformIntervalTrajectory(entry.getValue(), interval));
        }
        return result;
    }

    /**
     * Here we createAnnotation a regularly spaced trajectory in one dimension.
     * We achieve this by performing linear interpolation between points.
     * This relies on the assumption that the other dimensions translate in a linear way.
     * For example if we are making a regularly spaced trajectory in the temporal dimension
     * it means we assume that the entity which created the trajectory is moving at a constant speed between points.
     * (Which without speed information is the only fair assumption we can actually make.)
     *
     * @param trajectory the trajectory used a input for creating the regularly spaced version (it is not modified)
     * @param intervalMillis   the temporal interval (in milliseconds)
     * @return the regularly spaced trajectory
     */
    STTrajectory createUniformIntervalTrajectory(STTrajectory trajectory, long intervalMillis) {

        //make sure it is in cartesian mode
        trajectory.toCartesian();
        STTrajectory output = new STTrajectory(true, trajectory.getProjection());

        Iterator<STPt> iter = trajectory.iterator();
        if(!iter.hasNext()){return output;}

        LocalDateTime prevTime;
        double[] prevXY;
        {
            STPt startPt = iter.next();
            prevTime = startPt.getTime();
            prevXY = startPt.getCoords();
            output.add(startPt);
        }

        while(iter.hasNext()){
            STPt curPt = iter.next();
            long deltaMillis = ChronoUnit.MILLIS.between(prevTime, curPt.getTime());
            if(deltaMillis < intervalMillis){continue;}
            //case: the delta millis equal to or larger than the interval
            double alpha = intervalMillis/deltaMillis;
            double[] interpPt = Maths.lerp(prevXY, curPt.getCoords(), alpha);
            prevTime = prevTime.plus(intervalMillis, ChronoUnit.MILLIS);
            prevXY = interpPt;
            output.addGeographic(prevXY, prevTime);
        }

        return output;

    }

}
