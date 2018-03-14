package onethreeseven.trajsuite.osm.model.tag;

/**
 * A place producing or processing customized goods.
 * @author Luke Bermingham
 */
public class Craft extends OsmTag {
    public Craft(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "craft";
    }
}
