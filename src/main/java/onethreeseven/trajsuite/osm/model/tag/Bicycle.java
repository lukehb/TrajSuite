package onethreeseven.trajsuite.osm.model.tag;

/**
 * Indicating bikes go here (or not)
 */
public class Bicycle extends OsmTag {
    public Bicycle(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.BETTER_THAN_NOTHING;
    }

    @Override
    public String getName() {
        return "bicycle";
    }
}
