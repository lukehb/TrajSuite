package onethreeseven.trajsuite.osm.model.tag;

/**
 * For harbours.
 * @author Luke Bermingham
 */
public class Harbour extends OsmTag {
    public Harbour(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "harbour";
    }
}
