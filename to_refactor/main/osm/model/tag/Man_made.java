package onethreeseven.trajsuite.osm.model.tag;

/**
 * For identifying man-made (artificial) structures added to the landscape.
 * @author Luke Bermingham
 */
public class Man_made extends OsmTag {
    public Man_made(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.GENERIC;
    }

    @Override
    public String getName() {
        return "man_made";
    }
}
