package onethreeseven.trajsuite.osm.model.tag;

/**
 * It is a building like: residential, church, university
 */
public class Building extends OsmTag {
    public Building(String value) {
        super(value);
    }

    @Override
    public String getName() {
        return "building";
    }

    @Override
    Specificity getSpecificityImpl() {
		if(value.equals("yes")){
			return Specificity.BETTER_THAN_NOTHING;
		}
		return Specificity.SPECIFIC;
    }
}
