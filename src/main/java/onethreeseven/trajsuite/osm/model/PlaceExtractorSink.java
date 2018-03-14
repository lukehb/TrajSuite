package onethreeseven.trajsuite.osm.model;

import com.graphhopper.coll.GHLongObjectHashMap;
import com.vividsolutions.jts.geom.*;
import onethreeseven.geo.model.LatLonBounds;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Extracts ways from OSM data using a Consumer function.
 * @author Luke Bermingham
 */
public class PlaceExtractorSink implements Sink {

    private final GHLongObjectHashMap<double[]> nodeMap = new GHLongObjectHashMap<>();
    private final LatLonBounds studyRegion;
    private final AbstractGeographicProjection projection;
    private final GeometryFactory gf;
    private final BiConsumer<Entity, Geometry> entityProcessor;


    public PlaceExtractorSink(AbstractGeographicProjection projection, LatLonBounds studyRegion, BiConsumer<Entity, Geometry> wayProcessor){
        this.projection = projection;
        this.studyRegion = studyRegion;
        this.entityProcessor = wayProcessor;
        this.gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
    }

    private void processNode(NodeContainer nc){
        Node n = nc.getEntity();

        long osmNodeId = n.getId();
        double lat = n.getLatitude();
        double lon = n.getLongitude();
        if(studyRegion.contains(lat, lon)){
            double[] coordArr = projection.geographicToCartesian(lat, lon);

            boolean hasName = false;
            for (Tag tag : n.getTags()) {
                if(tag.getKey().contains("name")){
                    hasName = true;
                    break;
                }
            }
            //case: it has a name, so it a place itself
            if(hasName){
                //make a 1m rect around a single node place
                double hyp = 1;
                double xOffset = Math.cos(Math.PI*0.25) * hyp;
                double yOffset = Math.sin(Math.PI*0.25) * hyp;
                double minX = coordArr[0] - xOffset;
                double minY = coordArr[1] - yOffset;
                double maxX = coordArr[0] + xOffset;
                double maxY = coordArr[1] + yOffset;
                Geometry nodeGeom = gf.toGeometry(new Envelope(minX, maxX, minY, maxY));
                entityProcessor.accept(n, nodeGeom);
            }
            //case: it doesn't have a name, so it a node making up a way
            else{
                nodeMap.put(osmNodeId, coordArr);
            }
        }
    }

    private void processWay(WayContainer wc){
        Way way = wc.getEntity();
        Geometry wayGeom = getWayGeometry(way);
        if(wayGeom == null){return;}
        entityProcessor.accept(way, wayGeom);
    }

    private Geometry getWayGeometry(Way way){
        List<WayNode> wayNodes = way.getWayNodes();
        CoordinateList coordList = new CoordinateList();
        for (WayNode wayNode : wayNodes) {
            double[] pos = nodeMap.get(wayNode.getNodeId());
            if(pos == null){continue;}
            coordList.add(new Coordinate(pos[0], pos[1]), true);
        }
        if(coordList.isEmpty()){
            return null;
        }
        Coordinate[] coordArr = coordList.toCoordinateArray();

        //only handle polygon
        if(coordArr.length >= 4 && coordArr[0].equals2D(coordArr[coordArr.length-1])){
            Geometry geom = gf.createPolygon(coordArr);
            if(geom.getCoordinates().length > 10){
                return geom.getEnvelope();
            }
        }
        return null;
    }

    @Override
    public void process(EntityContainer entityContainer) {
        if(entityContainer instanceof NodeContainer){
            processNode((NodeContainer) entityContainer);
        }
        else if(entityContainer instanceof WayContainer){
            processWay((WayContainer) entityContainer);
        }
    }

    @Override
    public void initialize(Map<String, Object> metaData) {

    }

    @Override
    public void complete() {

    }

    @Override
    public void release() {

    }

}
