package onethreeseven.trajsuite.osm.model.tag;

/**
 * For buildings and land used by the Army, Navy, Air Force
 * and any other kind of military or paramilitary in the host country.
 * @author Luke Bermingham
 */
public class Military extends OsmTag {
    public Military(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.EXTREMELY_SPECIFIC;
    }

    @Override
    public String getName() {
        return "military";
    }
}
