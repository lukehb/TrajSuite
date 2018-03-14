package onethreeseven.trajsuite.osm.model.tag;

/**
 * Marks the location of a shop.
 * @author Luke Bermingham
 */
public class Shop extends OsmTag {
    public Shop(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "shop";
    }
}
