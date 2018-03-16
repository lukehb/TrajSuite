package onethreeseven.trajsuite.osm.model.tag;

/**
 * A barrier is a physical structure which blocks or impedes movement.
 * @author Luke Bermingham
 */
public class Barrier extends OsmTag{
    public Barrier(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.USELESS;
    }

    @Override
    public String getName() {
        return "barrier";
    }
}
