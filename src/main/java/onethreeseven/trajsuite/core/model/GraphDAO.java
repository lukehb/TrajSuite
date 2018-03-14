package onethreeseven.trajsuite.core.model;

import onethreeseven.geo.model.LatLonBounds;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import java.util.PrimitiveIterator;

/**
 * Contract for all graph data providers.
 * @author Luke Bermingham
 */
public interface GraphDAO {

    double getLat(int nodeId);
    double getLon(int nodeId);

    default LatLonBounds getSector(PrimitiveIterator.OfInt nodeIds){
        double minLat = Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double maxLon = Double.MIN_VALUE;

        while(nodeIds.hasNext()){
            int nodeId = nodeIds.next();
            double lat = getLat(nodeId);
            if(lat > maxLat){maxLat = lat;}
            if(lat < minLat){minLat = lat;}

            double lon = getLon(nodeId);
            if(lon > maxLon){maxLon = lon;}
            if(lon < minLon){minLon = lon;}
        }
        return new LatLonBounds(minLat, maxLat, minLon, maxLon);
    }

    default double[] getCoordinates(AbstractGeographicProjection projection, int nodeId){
        return projection.geographicToCartesian(getLat(nodeId), getLon(nodeId));
    }

}
