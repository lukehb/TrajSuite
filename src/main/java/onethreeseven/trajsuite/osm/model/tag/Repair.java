package onethreeseven.trajsuite.osm.model.tag;

/**
 * For Shops that offer repair of goods (e.g. computers).
 * @author Luke Bermingham
 */
public class Repair extends OsmTag {
    public Repair(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "repair";
    }
}
