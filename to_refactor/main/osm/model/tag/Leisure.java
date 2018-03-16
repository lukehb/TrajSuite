package onethreeseven.trajsuite.osm.model.tag;

/**
 * Place for leisure: parks, pools, sporting pitches
 */
public class Leisure extends OsmTag {
    public Leisure(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "leisure";
    }
}
