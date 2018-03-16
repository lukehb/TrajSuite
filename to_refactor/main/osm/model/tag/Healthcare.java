package onethreeseven.trajsuite.osm.model.tag;

/**
 * All places that provide healthcare (are part of the healthcare sector).
 * @author Luke Bermingham
 */
public class Healthcare extends OsmTag {
    public Healthcare(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "healthcare";
    }
}
