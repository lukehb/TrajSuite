package onethreeseven.trajsuite.osm.model;


import onethreeseven.common.model.TimeCategory;
import onethreeseven.trajsuite.osm.model.tag.Unknown;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A {@link TimeCategory} and semantic {@link SemanticPlace}.
 * @author Luke Bermingham
 */
public class TimeAndPlace {

    private LocalDateTime actualTime;
    private TimeCategory timeCategory;
    private SemanticPlace place;

    public TimeAndPlace(LocalDateTime actualTime, TimeCategory time, SemanticPlace place) {
        this.actualTime = actualTime;
        this.timeCategory = time;
        this.place = place;
    }

    public void setTimeCategory(TimeCategory timeCategory) {
        this.timeCategory = timeCategory;
    }

    public void setPlace(SemanticPlace place) {
        this.place = place;
    }

    public TimeCategory getTimeCategory() {
        return timeCategory;
    }

    public LocalDateTime getActualTime() {
        return actualTime;
    }

    public void setActualTime(LocalDateTime actualTime) {
        this.actualTime = actualTime;
    }

    public SemanticPlace getPlace() {
        return place;
    }

    private static final SemanticPlace nullPlace = new SemanticPlace("null", "null", new Unknown("null"));

    public String print(String delimiter) {
        String timeStamp = this.actualTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return timeStamp + delimiter +
                timeCategory + delimiter +
                ((place == null) ? nullPlace.print(delimiter) : place.print(delimiter));
    }
}
