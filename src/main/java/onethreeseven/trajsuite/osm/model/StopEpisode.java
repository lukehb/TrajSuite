package onethreeseven.trajsuite.osm.model;

import onethreeseven.common.model.TimeCategory;
import onethreeseven.datastructures.model.STPt;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Represents a spatio-temporal stop in a movement sequence.
 * The stop is located at a specific geographic position with a
 * radius (for spatial uncertainty and small inter-stop movements)
 * and stop begin and end time.
 * @author Luke Bermingham
 */
public class StopEpisode extends STPt {

    private final double stopRadiusMeters;
    private final TimeCategory timeCategory;

    public StopEpisode(double[] centerCoords,
                       double stopRadiusMeters,
                       LocalDateTime startEpisode,
                       TimeCategory timeCategory) {
        super(centerCoords, startEpisode);
        this.stopRadiusMeters = Math.max(1, stopRadiusMeters);
        this.timeCategory = timeCategory;
    }

    public TimeCategory getTimeCategory() {
        return timeCategory;
    }

    public double getStopRadiusMeters() {
        return stopRadiusMeters;
    }

}
