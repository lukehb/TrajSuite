package onethreeseven.trajsuite.osm.model.tag;

/**
 * A club is an association of two or more people
 * united by a common interest or goal.
 * @author Luke Bermingham
 */
public class Club extends OsmTag {
    public Club(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.EXTREMELY_SPECIFIC;
    }

    @Override
    public String getName() {
        return "club";
    }
}
