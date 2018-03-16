package onethreeseven.trajsuite.osm.model.tag;

/**
 * The type of environment (good for animal tracking)
 */
public class Natural extends OsmTag {
    public Natural(String value) {
        super(value);
    }

    @Override
    public String getName() {
        return "natural";
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.BETTER_THAN_NOTHING;
    }
}
