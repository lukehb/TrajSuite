package onethreeseven.trajsuite.osm.model.tag;

/**
 * A place with some utility: post-box, restaurant, place of worship etc
 */
public class Amenity extends OsmTag{

    public Amenity(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "amenity";
    }
}
