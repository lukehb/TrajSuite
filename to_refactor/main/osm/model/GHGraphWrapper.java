package onethreeseven.trajsuite.osm.model;

import onethreeseven.geo.model.LatLonBounds;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.trajsuite.core.model.GeographicGraph;

import java.nio.DoubleBuffer;
import java.util.Iterator;

/**
 * Simple wrapper over gh dao used mostly for rendering.
 * @author Luke Bermingham
 */
public class GHGraphWrapper extends GeographicGraph {

    private final GraphHopperDAO dao;
    private final AbstractGeographicProjection projection;

    public GHGraphWrapper(GraphHopperDAO dao, AbstractGeographicProjection projection) {
        super(dao.getBoundingSector(), projection);
        this.dao = dao;
        this.projection = projection;
    }

    @Override
    public DoubleBuffer getEdges(LatLonBounds viewBounds) {
        return dao.getAllEdgesWithinSector(projection, viewBounds);
    }


    @Override
    public Iterator<double[]> coordinateIter() {
        throw new UnsupportedOperationException("Cannot iterate the coordinate of a graph that changes with the viewport.");
    }

    @Override
    public Iterator<double[]> geoCoordinateIter() {
        throw new UnsupportedOperationException("Cannot iterate the coordinate of a graph that changes with the viewport.");
    }
}
