package onethreeseven.trajsuite.osm.model.tag;

/**
 * For mapping electrical power lines and associated infrastructure.
 * @author Luke Bermingham
 */
public class Power extends OsmTag {
    public Power(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.BETTER_THAN_NOTHING;
    }

    @Override
    public String getName() {
        return "power";
    }
}
