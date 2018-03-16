package onethreeseven.trajsuite.osm.model.tag;

/**
 * Used to identify features that are of historic interest.
 * @author Luke Bermingham
 */
public class Historic extends OsmTag {
    public Historic(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.GENERIC;
    }

    @Override
    public String getName() {
        return "historic";
    }
}
