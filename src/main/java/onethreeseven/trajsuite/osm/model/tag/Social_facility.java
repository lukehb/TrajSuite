package onethreeseven.trajsuite.osm.model.tag;

/**
 * A social facility is any place where social services are conducted.
 * @author Luke Bermingham
 */
public class Social_facility extends OsmTag{
    public Social_facility(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "social_facility";
    }
}
