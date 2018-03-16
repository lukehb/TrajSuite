package onethreeseven.trajsuite.osm.model;


import onethreeseven.datastructures.model.STPt;

import java.time.LocalDateTime;

/**
 * A semantic point that contains the coordinates of the
 * pt and the semantic {@link TimeAndPlace} information.
 * @author Luke Bermingham
 */
public class SemanticPt extends STPt {

    private final TimeAndPlace timeAndPlace;

    SemanticPt(double[] coords, TimeAndPlace timeAndPlace) {
        super(coords, timeAndPlace.getActualTime());
        this.timeAndPlace = timeAndPlace;
    }

    @Override
    public String printExtra(String delimiter) {
        return timeAndPlace.print(delimiter);
    }

    public double[] getCoords(){
        return super.coords;
    }

    public TimeAndPlace getTimeAndPlace(){
        return timeAndPlace;
    }
}
