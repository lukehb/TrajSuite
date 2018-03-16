package onethreeseven.trajsuite.osm.model.tag;

/**
 * Used to identify pedestrian areas
 */
public class Foot extends OsmTag {
    public Foot(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.BETTER_THAN_NOTHING;
    }

    @Override
    public String getName() {
        return "foot";
    }
}
