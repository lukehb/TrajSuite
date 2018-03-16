package onethreeseven.trajsuite.osm.model.tag;

/**
 * Tunnel is used for roads, railway line, canals etc
 * that run underground (in tunnel).
 * @author Luke Bermingham
 */
public class Tunnel extends OsmTag {
    public Tunnel(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.BETTER_THAN_NOTHING;
    }

    @Override
    public String getName() {
        return "tunnel";
    }
}
