package onethreeseven.trajsuite.osm.data;

import onethreeseven.trajsuite.osm.model.SemanticPlace;
import onethreeseven.trajsuite.osm.model.tag.OsmTag;
import onethreeseven.trajsuite.osm.model.tag.OsmTagFilter;

/**
 * Resolve a {@link String[]} to a {@link SemanticPlace} using the given indices.
 * @author Luke Bermingham
 */
public class SemanticPlaceFieldsResolver {

    private final int placeIdIdx;
    private final int placeNameIdx;
    private final int placeTypeIdx;

    public SemanticPlaceFieldsResolver(int placeIdx, int placeNameIdx, int placeTypeIdx) {
        this.placeIdIdx = placeIdx;
        this.placeNameIdx = placeNameIdx;
        this.placeTypeIdx = placeTypeIdx;
    }

    public SemanticPlace resolve(String[] in) {
        String placeId = in[placeIdIdx].trim();
        String placeName = in[placeNameIdx].trim();
        String placeType = in[placeTypeIdx].trim();
        //for place type we are expecting key-value pair delimited by an equals sign, like:
        //amenity=university
        String[] kv = placeType.split("=");

        String placeKey = kv[0];
        String placeValue = "";
        if(kv.length == 2){
            placeValue = kv[1];
        }
        OsmTag placeTag = OsmTagFilter.ALL.resolve(placeKey, placeValue);
        return new SemanticPlace(placeId, placeName, placeTag);


    }
}
