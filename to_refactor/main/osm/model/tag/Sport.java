package onethreeseven.trajsuite.osm.model.tag;

/**
 * Used to identify one or more sports
 * which can be played within or on some physical feature.
 * @author Luke Bermingham
 */
public class Sport extends OsmTag {
    public Sport(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.EXTREMELY_SPECIFIC;
    }

    @Override
    public String getName() {
        return "sport";
    }
}
