package onethreeseven.trajsuite.osm.model.tag;

/**
 * For an unknown tag.
 * @author Luke Bermingham
 */
public class Unknown extends OsmTag {
    public Unknown(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.USELESS;
    }

    @Override
    public String getName() {
        return "Unknown";
    }
}
