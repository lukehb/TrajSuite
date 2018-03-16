package onethreeseven.trajsuite.osm.model.tag;

/**
 * A place predominantly selling services.
 * @author Luke Bermingham
 */
public class Office extends OsmTag {
    public Office(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "office";
    }
}
