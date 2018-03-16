package onethreeseven.trajsuite.osm.util;


import onethreeseven.datastructures.model.CompositePt;
import onethreeseven.trajsuite.osm.model.SemanticPlace;
import java.util.Collection;

/**
 * Interface for a services that provides places based on geographic coordinates.
 * @author Luke Bermingham
 */
public abstract class PlaceService {

    PlaceService(){}

    public abstract Collection<CompositePt<SemanticPlace>> getNearbyPlaces(double lat, double lon, int searchRadius);

    public abstract CompositePt<SemanticPlace> getClosestPlace(double lat, double lon, int searchRadius);

}
