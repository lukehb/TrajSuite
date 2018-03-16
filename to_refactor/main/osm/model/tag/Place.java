package onethreeseven.trajsuite.osm.model.tag;

/**
 * Town, suburb, hamlet, village etc.
 */
public class Place extends OsmTag {
    public Place(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.GENERIC;
    }

    @Override
    public String getName() {
        return "place";
    }
}
