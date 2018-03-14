package onethreeseven.trajsuite.core.algorithm;

import onethreeseven.datastructures.model.STTrajectory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class wraps another interval transformation algorithm to perform
 * a temporal interval transform whilst rounding the result.
 * Under the hood all trajectory temporal information is typically stored as
 * millis for granularity reasons and this algorithm doesn't change that.
 * But what it does do is effectively round the stored millis into some
 * other time unit. For example we may represent an entry's temporal value as
 * 2137, which we treat as millisecond, but what if we want the interval to only
 * be in seconds (but keep our underlying millis representation)? Well, this algorithm's
 * solution is to round it to whatever time unit is specified, for example if the
 * specified the time unit as seconds, then 2137 millis will be rounded to 2000 millis
 * because that the nearest amount of seconds (i.e 2 seconds).
 * This algorithm can wrap any of the interval transforms that implement the abstract
 * base class,
 * @see AbstractTemporalIntervalTransform
 * @author Luke Bermingham.
 */
public class TimeUnitTemporalInterval extends AbstractTemporalIntervalTransform {

    private final AbstractTemporalIntervalTransform algo;
    private final TimeUnit timeUnit;

    private TimeUnitTemporalInterval(AbstractTemporalIntervalTransform algo, TimeUnit timeUnit) {
        this.algo = algo;
        this.timeUnit = timeUnit;
    }

    public static TimeUnitTemporalInterval wrap(AbstractTemporalIntervalTransform algo, TimeUnit timeUnit) {
        return new TimeUnitTemporalInterval(algo, timeUnit);
    }

    @Override
    long calculateTemporalInterval(Map<String, STTrajectory> trajectories) {
        long millis = algo.calculateTemporalInterval(trajectories);
        //convert resolve our milliseconds value to whatever the user wants
        long nUnits = timeUnit.convert(millis, TimeUnit.MILLISECONDS);
        //we have enough millis to go the some other unit, so
        //now convert it back to millis (effectively rounding it)
        return TimeUnit.MILLISECONDS.convert(nUnits, timeUnit);
    }
}
