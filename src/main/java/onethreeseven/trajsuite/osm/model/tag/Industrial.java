package onethreeseven.trajsuite.osm.model.tag;

/**
 * Describing the type of industry.
 * @author Luke Bermingham
 */
public class Industrial extends OsmTag {
    public Industrial(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.GENERIC;
    }

    @Override
    public String getName() {
        return "industrial";
    }
}
