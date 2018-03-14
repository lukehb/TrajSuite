package onethreeseven.trajsuite.osm.model.tag;

/**
 * For bridges.
 * @author Luke Bermingham
 */
public class Bridge extends OsmTag {
    public Bridge(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.BETTER_THAN_NOTHING;
    }

    @Override
    public String getName() {
        return "bridge";
    }
}
