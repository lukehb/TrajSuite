package onethreeseven.trajsuite.osm.model;

import java.util.ArrayList;

/**
 * A container for many {@link SemanticPlace}
 * @author Luke Bermingham
 */
public class CompositeSemanticPlace extends SemanticPlace {

    private final ArrayList<SemanticPlace> places;

    public CompositeSemanticPlace(SemanticPlace a, SemanticPlace b) {
        super(a.id, a.placeName, a.primaryTag);
        this.places = new ArrayList<>();
        this.places.add(a);
        this.places.add(b);
    }

    public void add(SemanticPlace place){
        this.places.add(place);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (SemanticPlace place : places) {
            sb.append(place.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public String print(String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (SemanticPlace place : places) {
            sb.append(place.print(delimiter));
            sb.append("\n");
        }
        return sb.toString();
    }

    public ArrayList<SemanticPlace> getPlaces() {
        return places;
    }
}
