package onethreeseven.trajsuite.osm.model.tag;

/**
 * This key describes how a specific junction is constituted.
 * @author Luke Bermingham
 */
public class Junction extends OsmTag {
    public Junction(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.USELESS;
    }

    @Override
    public String getName() {
        return "junction";
    }
}
