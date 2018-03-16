package onethreeseven.trajsuite.osm.model.tag;

/**
 * For describing the type of food served at an eating place.
 * @author Luke Bermingham
 */
public class Cuisine extends OsmTag {
    public Cuisine(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "cuisine";
    }
}
