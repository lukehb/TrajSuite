package onethreeseven.trajsuite.osm.model.tag;

/**
 * Denotes stop positions and platforms of public transport.
 * @author Luke Bermingham
 */
public class Public_transport extends OsmTag {
    public Public_transport(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "public_transport";
    }
}
