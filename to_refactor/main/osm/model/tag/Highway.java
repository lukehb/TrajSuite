package onethreeseven.trajsuite.osm.model.tag;

/**
 * Things travel along this, i.e: road, footpath, motorway
 */
public class Highway extends OsmTag {
    public Highway(String value) {
        super(value);
    }

    @Override
    public String getName() {
        return "highway";
    }

    @Override
    Specificity getSpecificityImpl() {
        switch(value){
            case "bus_stop":
                return Specificity.SPECIFIC;
        }
        return Specificity.BETTER_THAN_NOTHING;
    }
}
