package onethreeseven.trajsuite.osm.graphics;


import gov.nasa.worldwind.layers.LayerList;
import onethreeseven.trajsuite.core.graphics.GLGeoGraph;
import onethreeseven.trajsuite.osm.model.GHGraphWrapper;
import onethreeseven.trajsuitePlugin.graphics.GraphicsPayload;

/**
 * A stub class around {@link onethreeseven.trajsuite.core.graphics.GLGeoGraph}.
 * @author Luke Bermingham
 */

public class GLGHGraph extends GLGeoGraph {
    public GLGHGraph(GHGraphWrapper model, GraphicsPayload graphicsPayload, LayerList ll) {
        super(model, graphicsPayload, ll);
    }
}
