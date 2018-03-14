package onethreeseven.trajsuite.osm.model.tag;

/**
 * Used to mark rooms inside a building.
 * @author Luke Bermingham
 */
public class Room extends OsmTag {
    public Room(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.GENERIC;
    }

    @Override
    public String getName() {
        return "room";
    }
}
