package onethreeseven.trajsuite.core.model;

import onethreeseven.geo.model.LatLonBounds;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;

import java.nio.DoubleBuffer;

/**
 * Represents a graph that made up of geographical nodes and edges (model used for rendering).
 * @author Luke Bermingham
 */
public abstract class GeographicGraph implements BoundingCoordinates {

    protected final LatLonBounds sector;
    protected final double[][] bounds;

    public GeographicGraph(LatLonBounds sector, AbstractGeographicProjection projection){
        this.sector = sector;
        this.bounds = BoundsUtil.fromLatLonBounds(sector, projection);
    }

    /**
     * Gets the edges that are visible within the sector.
     * Edges are packed in node pairs. Edge AB and BC are packed ABBC.
     * @param viewBounds the visible bounds
     * @return the packed edges in graphics coordinates (ready for rendering).
     */
    public abstract DoubleBuffer getEdges(LatLonBounds viewBounds);

    public LatLonBounds getSector(){
        return sector;
    }

    @Override
    public double[][] getBounds() {
        return bounds;
    }

    @Override
    public LatLonBounds getLatLonBounds() {
        return getSector();
    }



}
