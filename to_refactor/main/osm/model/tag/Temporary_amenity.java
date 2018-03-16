package onethreeseven.trajsuite.osm.model.tag;

/**
 * Like {@link Amenity} - but temporary.
 * @author Luke Bermingham
 */
public class Temporary_amenity extends OsmTag {
    public Temporary_amenity(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.BETTER_THAN_NOTHING;
    }

    @Override
    public String getName() {
        return "temporary_amenity";
    }
}
