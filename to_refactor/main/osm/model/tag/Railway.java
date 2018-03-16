package onethreeseven.trajsuite.osm.model.tag;

/**
 * Used to indicate all sorts of rail systems
 */
public class Railway extends OsmTag{

    public Railway(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "railway";
    }
}
