package onethreeseven.trajsuite.osm.util;


import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.Category;
import fi.foyt.foursquare.api.entities.CompactVenue;
import fi.foyt.foursquare.api.entities.VenuesSearchResult;
import onethreeseven.datastructures.model.CompositePt;
import onethreeseven.trajsuite.osm.model.SemanticPlace;
import onethreeseven.trajsuite.osm.model.tag.FourSquareToOSM;
import onethreeseven.trajsuite.osm.model.tag.OsmTag;
import onethreeseven.trajsuite.osm.model.tag.Unknown;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Small wrapper around FourSquare api to do some place requests.
 * @author Luke Bermingham
 */
public class FourSquarePlaceService extends PlaceService {

    private final FoursquareApi api;
    private final static String FIXED_INTENT = "browse";
    private final static int FIXED_RESULTS_LIMIT = 50;

    public FourSquarePlaceService(){

        String clientId = System.getenv("FOURSQUARE_ID");
        String secret = System.getenv("FOURSQUARE_SECRET");

        if(clientId == null || secret == null){
            throw new IllegalStateException("To instantiate this class you must have a valid FourSquare client id " +
                    "and client secret in your system variables called \"FOURSQUARE_ID\" and \"FOURSQUARE_SECRET\", " +
                    "respectively");
        }

        api = new FoursquareApi(clientId, secret, "None");
    }

    @Override
    public Collection<CompositePt<SemanticPlace>> getNearbyPlaces(double lat, double lon, int searchRadius) {
        String ll = lat + "," + lon;

        try {
            Result<VenuesSearchResult> results = api.venuesSearch(ll,
                    null,
                    null,
                    null,
                    null,
                    FIXED_RESULTS_LIMIT,
                    FIXED_INTENT,
                    null,
                    null,
                    null,
                    null,
                    searchRadius,
                    null);

            if(Integer.valueOf(results.getMeta().getRateLimitRemaining()) <= 0){
                throw new IllegalStateException("Have made too many requests to FourSquare within 24hrs. Rate limited.");
            }

            if(results.getMeta().getCode() != 200){
                String error = results.getMeta().getErrorType() + " " + results.getMeta().getErrorDetail();
                throw new IllegalStateException("Got a bad response from the FourSquare API. " + error);
            }

            ArrayList<CompositePt<SemanticPlace>> places = new ArrayList<>();

            CompactVenue[] venues = results.getResult().getVenues();
            {
                for (CompactVenue venue : venues) {
                    double venueDistance = venue.getLocation().getDistance();
                    if(venueDistance <= searchRadius){
                        SemanticPlace place = toPlace(venue);
                        places.add(new SpatioSemanticPt(
                                venue.getLocation().getLat(),
                                venue.getLocation().getLng(),
                                place));
                    }
                }
            }
            return places;
        } catch (FoursquareApiException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public CompositePt<SemanticPlace> getClosestPlace(double lat, double lon, int searchRadius) {

        String ll = lat + "," + lon;

        try {
            Result<VenuesSearchResult> results = api.venuesSearch(ll,
                    null,
                    null,
                    null,
                    null,
                    FIXED_RESULTS_LIMIT,
                    FIXED_INTENT,
                    null,
                    null,
                    null,
                    null,
                    searchRadius,
                    null);

            if(Integer.valueOf(results.getMeta().getRateLimitRemaining()) <= 0){
                throw new IllegalStateException("Have made too many requests to FourSquare within 24hrs. Rate limited.");
            }

            if(results.getMeta().getCode() != 200){
                String error = results.getMeta().getErrorType() + " " + results.getMeta().getErrorDetail();
                throw new IllegalStateException("Got a bad response from the FourSquare API. " + error);
            }

            CompactVenue[] venues = results.getResult().getVenues();
            //find closest venue
            CompactVenue closestVenue = null;
            {
                double closestDistance = Double.MAX_VALUE;
                for (CompactVenue venue : venues) {
                    double venueDistance = venue.getLocation().getDistance();
                    if(venueDistance < closestDistance && venueDistance <= searchRadius){
                        closestDistance = venueDistance;
                        closestVenue = venue;
                    }
                }
            }
            if(closestVenue == null){
                return null;
            }

            //using closest venue turn it into an OSM Tag
            SemanticPlace place = toPlace(closestVenue);
            return new SpatioSemanticPt(
                    closestVenue.getLocation().getLat(),
                    closestVenue.getLocation().getLng(),
                    place);

        } catch (FoursquareApiException e) {
            e.printStackTrace();
        }

        return null;
    }

    private SemanticPlace toPlace(CompactVenue venue){
        Category[] placeTypes = venue.getCategories();
        Category primary = null;
        for (Category placeType : placeTypes) {
            if(placeType.getPrimary()){
                primary = placeType;
                break;
            }
        }
        if(primary != null){
            OsmTag tag = FourSquareToOSM.categoryToTag(primary.getId());
            if(tag == null){
                tag = new Unknown("");
            }
            return new SemanticPlace(primary.getId(), venue.getName(), tag);
        }
        return null;
    }


    private class SpatioSemanticPt extends CompositePt<SemanticPlace>{

        private final SemanticPlace place;

        SpatioSemanticPt(double lat, double lon, SemanticPlace place) {
            super(new double[]{lat, lon});
            this.place = place;
        }

        @Override
        public SemanticPlace getExtra() {
            return place;
        }

        @Override
        public String printExtra(String delimiter) {
            return place.toString();
        }
    }

}
