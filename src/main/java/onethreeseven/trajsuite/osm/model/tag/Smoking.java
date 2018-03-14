package onethreeseven.trajsuite.osm.model.tag;

/**
 * A smoking area, but also the type of smoking area.
 * @author Luke Bermingham
 */
public class Smoking extends OsmTag {
    public Smoking(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.GENERIC;
    }

    @Override
    public String getName() {
        return "smoking";
    }
}
