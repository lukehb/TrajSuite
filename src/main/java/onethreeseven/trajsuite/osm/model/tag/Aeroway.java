package onethreeseven.trajsuite.osm.model.tag;

/**
 * Airports and all that goes with it
 */
public class Aeroway extends OsmTag {
    public Aeroway(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "aeroway";
    }
}
