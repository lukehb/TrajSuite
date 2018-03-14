package onethreeseven.trajsuite.osm.model.tag;

/**
 * The mooring tag marks an area of bank where boats
 * are explicitly permitted to moor.
 * @author Luke Bermingham
 */
public class Mooring extends OsmTag {
    public Mooring(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "mooring";
    }
}
