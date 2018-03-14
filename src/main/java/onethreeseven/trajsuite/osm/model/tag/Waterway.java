package onethreeseven.trajsuite.osm.model.tag;

/**
 * For moving water, i.e streams, rivers.
 */
public class Waterway extends OsmTag {

    public Waterway(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.BETTER_THAN_NOTHING;
    }

    @Override
    public String getName() {
        return "waterway";
    }
}
