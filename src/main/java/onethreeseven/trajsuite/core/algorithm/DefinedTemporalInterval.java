package onethreeseven.trajsuite.core.algorithm;

import onethreeseven.datastructures.model.STPt;
import onethreeseven.datastructures.model.STTrajectory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;

/**
 * Applies a specific temporal interval to all trajectory entries.
 * Note this method does not use interpolation between points.
 * Instead just increments the interval resolve starting at the value of the first point.
 * For example, if we are working with the temporal dimension of the trajectory
 * we may start at the first point with a time of 9:01am and our defined interval
 * is 1 minute so we the second entry's temporal value will become 9:02am, and so on.
 * @author Luke Bermingham.
 */
public class DefinedTemporalInterval extends AbstractTemporalIntervalTransform {

    private final long intervalMillis;
    public DefinedTemporalInterval(long intervalMillis){
        this.intervalMillis = intervalMillis;
    }

    @Override
    protected long calculateTemporalInterval(Map<String, STTrajectory> input) {
        return intervalMillis;
    }

    @Override
    protected STTrajectory createUniformIntervalTrajectory(STTrajectory trajectory, long interval) {

        STTrajectory out = new STTrajectory(true, trajectory.getProjection());
        Iterator<STPt> stPtIter = trajectory.iterator();

        if(!stPtIter.hasNext()){return out;}

        STPt prevPt = stPtIter.next();
        LocalDateTime prevTime = prevPt.getTime();
        {
            double[] startPt = new double[2];
            System.arraycopy(prevPt.getCoords(), 0, startPt, 0, 2);
            out.addGeographic(startPt, prevTime);
        }

        while(stPtIter.hasNext()){
            STPt curPt = stPtIter.next();
            double[] xy = curPt.getCoords();
            double[] xyCopy = new double[xy.length];
            //copy contents
            System.arraycopy(xy, 0, xyCopy, 0, xy.length);
            prevTime = prevTime.plus(interval, ChronoUnit.MILLIS);
            out.addGeographic(xyCopy, prevTime);
        }
        return out;
    }
}
