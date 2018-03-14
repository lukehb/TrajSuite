package onethreeseven.trajsuite.osm.model.tag;

/**
 * Tourist things: zoo, hotel, attraction etc
 */
public class Tourism extends OsmTag {
    public Tourism(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "tourism";
    }
}
