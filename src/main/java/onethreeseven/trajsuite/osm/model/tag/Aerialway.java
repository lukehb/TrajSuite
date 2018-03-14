package onethreeseven.trajsuite.osm.model.tag;

/**
 * Used for various forms of transport for passengers and goods that use
 * wires, including cable-cars, chair-lifts and drag-lifts.
 * @author Luke Bermingham
 */
public class Aerialway extends OsmTag {

    public Aerialway(String value) {
        super(value);
    }

    @Override
    Specificity getSpecificityImpl() {
        return Specificity.SPECIFIC;
    }

    @Override
    public String getName() {
        return "aerialway";
    }
}
