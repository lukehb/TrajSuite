package onethreeseven.trajsuite.osm.model.tag;

/**
 * A generic closed area.
 * @author Luke Bermingham
 */
public class Area extends OsmTag {
    public Area(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.USELESS;
    }

    @Override
    public String getName() {
        return "area";
    }
}
