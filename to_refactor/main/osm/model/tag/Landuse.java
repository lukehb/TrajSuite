package onethreeseven.trajsuite.osm.model.tag;

/**
 * I.e residential, forest, or farm etc.
 */
public class Landuse extends OsmTag {
    public Landuse(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.BETTER_THAN_NOTHING;
    }

    @Override
    public String getName() {
        return "landuse";
    }
}
