/**
 * Created by Luke on 29/10/2015.
 * Copyright 137Industries
 */

package onethreeseven.trajsuite.osm.model;

import com.graphhopper.GraphHopper;
import com.graphhopper.coll.GHBitSet;
import com.graphhopper.coll.GHBitSetImpl;
import com.graphhopper.matching.*;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.*;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.*;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.GHPoint3D;
import gov.nasa.worldwind.globes.Globe;
import onethreeseven.collections.DoubleArray;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.model.SpatialTrajectory;
import onethreeseven.geo.model.LatLonBounds;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.geo.projection.ProjectionMercator;
import onethreeseven.trajsuite.core.model.GraphDAO;
import onethreeseven.trajsuite.osm.algorithm.MarkovMapMatching;
import onethreeseven.trajsuitePlugin.settings.PluginSettings;
import java.io.File;
import java.nio.DoubleBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;


/**
 * DAO for working the graph hopper.
 */
public class GraphHopperDAO implements GraphDAO {

    private final File workingDir;
    private final Logger logger;
    private final FlagEncoder encoder;
    private GraphHopper gh;

    public GraphHopperDAO(File osmFile) {
        this.logger = Logger.getLogger(GraphHopperDAO.class.getSimpleName());
        this.workingDir = FileUtil.makeAppDir("graphHopper_" + osmFile.getName());
        logger.info("Putting graph hopper data into directory: " + workingDir.getAbsolutePath());
        encoder = new CarFlagEncoder();
        load(osmFile);
    }

    private void load(File osmFile) {
        logger.info("Preparing to load osm file: " + osmFile);
        // import OpenStreetMap data
        gh = new GraphHopperOSM().setOSMFile(osmFile.getAbsolutePath());
        //no simplification
        //gh.setWayPointMaxDistance(0);
        gh.setGraphHopperLocation(workingDir.getAbsolutePath());
        gh.setEncodingManager(new EncodingManager(encoder));
        gh.setCHEnabled(true);
        gh.importOrLoad();
        logger.info("Done loading osm file into graph hopper.");
    }

    /**
     * Try to match gpx file to underlying graph
     *
     * @param gpxFile the gpx file to match
     * @return the edges that have been matched to
     */
    public MatchResult mapMatch(GPXFile gpxFile, boolean fastMode) {

        GraphHopperStorage graph = gh.getGraphHopperStorage();

        MarkovMapMatching mapMatching = new MarkovMapMatching(graph, (LocationIndexTree) gh.getLocationIndex(), encoder);
        mapMatching.setOnlyExpandBest(fastMode);

        long startTime = System.currentTimeMillis();
        MatchResult result = mapMatching.doWork(gpxFile.getEntries());
        logger.info("Performed matching in: " + (System.currentTimeMillis() - startTime) + " millis.");
        return result;
    }

    public MatchResult ghMapMatch(GPXFile gpxFile){

        ShortestWeighting weighting = new ShortestWeighting(encoder);

        AlgorithmOptions opts = AlgorithmOptions.start()
                .weighting(weighting)
                .algorithm(Parameters.Algorithms.ASTAR)
                .traversalMode(TraversalMode.NODE_BASED)
                .build();

        MapMatching mapMatching = new MapMatching(gh, opts);

        return mapMatching.doWork(gpxFile.getEntries());
    }


    /**
     * Converts edge matches to a trajectory
     * @param matches the edge matches resolve map-matching
     * @param globe the globe to convert them resolve geographic to world.
     * @return a trajectory
     */
    public SpatialTrajectory matchesToTrajectory(List<EdgeMatch> matches, Globe globe){
        NodeAccess na = gh.getGraphHopperStorage().getNodeAccess();
        SpatialTrajectory trajectory = new SpatialTrajectory(true, new ProjectionMercator());
        for (EdgeMatch match : matches) {
            EdgeIteratorState edgeIter = match.getEdgeState();
            int baseNode = edgeIter.getBaseNode();
            int adjNode = edgeIter.getAdjNode();
            trajectory.addGeographic(new double[]{na.getLat(baseNode), na.getLon(baseNode)});
            trajectory.addGeographic(new double[]{na.getLat(adjNode), na.getLon(adjNode)});
        }
        return trajectory;
    }

    public List<QueryResult> queryAt(double lat, double lon, int within){
        LocationIndexMatch indexMatch = new LocationIndexMatch(gh.getGraphHopperStorage(), (LocationIndexTree) gh.getLocationIndex());
        return indexMatch.findNClosest(lat, lon, new DefaultEdgeFilter(encoder), within);
    }


    /**
     * @return The bounding box sector around the graph nodes and edges in this
     * graph hopper instance.
     */
    LatLonBounds getBoundingSector() {
        BBox bb = gh.getGraphHopperStorage().getBounds();
        return new LatLonBounds(bb.minLat, bb.maxLat, bb.minLon, bb.maxLon);
    }

    /**
     * Given a sector find any node in the graph that falls within it
     *
     * @param sector the search sector
     * @return a node id within the sector, or -1 if no node is found
     */
    private int getAnyNodeWithin(LatLonBounds sector) {
        LatLonBounds graphSector = getBoundingSector();
        if (!graphSector.contains(sector) && !sector.intersects(graphSector)) {
            return -1;
        }
        final int lastId = gh.getGraphHopperStorage().getNodes();
        final NodeAccess na = gh.getGraphHopperStorage().getNodeAccess();

        for (int nodeIdx = 0; nodeIdx < lastId; nodeIdx++) {
            double lat = na.getLat(nodeIdx);
            double lon = na.getLon(nodeIdx);
            if (sector.contains(lat, lon)) {
                return nodeIdx;
            }
        }
        return -1;
    }

    DoubleBuffer getAllEdgesWithinSector(final AbstractGeographicProjection projection, final LatLonBounds sector) {

        final NodeAccess na = gh.getGraphHopperStorage().getNodeAccess();
        final EdgeExplorer edgeExplorer = gh.getGraphHopperStorage().createEdgeExplorer();

        int startNode = getAnyNodeWithin(sector);
        if (startNode >= 0) {

            final DoubleArray out = new DoubleArray(1000, true);

            final Consumer<Integer> nodeProcessor = new Consumer<Integer>() {

                private final GHBitSet processedEdges = new GHBitSetImpl();

                private double[] latlonToCoordinates(double lat, double lon){
                    return projection.geographicToCartesian(lat, lon);
                }

                private double[] nodeToCoordinates(int nodeId){
                    return latlonToCoordinates(na.getLat(nodeId), na.getLon(nodeId));
                }

                @Override
                public void accept(final Integer baseNode) {
                    final double smallElevation = PluginSettings.smallElevation.getSetting();

                    EdgeIterator edgeIter = edgeExplorer.setBaseNode(baseNode);

                    while(edgeIter.next()){
                        //get the adjacent node
                        int adjNode = edgeIter.getAdjNode();
                        int sum = baseNode + adjNode;
                        if (!processedEdges.contains(sum)) {
                            processedEdges.add(sum);
                            //always push base node because we are storing each full edge
                            double[] baseNodeCoords = nodeToCoordinates(baseNode);
                            out.addAll(baseNodeCoords);
                            boolean needsElevation = baseNodeCoords.length == 2;
                            if(needsElevation){
                                out.add(smallElevation);
                            }

                            //store any edges in between base and adj node that may be way geometry
                            PointList pointList = edgeIter.fetchWayGeometry(0);
                            for (GHPoint3D wayPoint : pointList) {
                                double[] coords = latlonToCoordinates(wayPoint.lat, wayPoint.lon);
                                //push it twice because we are storing full edges (not sequences)
                                for (int i = 0; i < 2; i++) {
                                    out.addAll(coords);
                                    if(needsElevation){
                                        out.add(smallElevation);
                                    }
                                }
                            }
                            out.addAll(nodeToCoordinates(adjNode));
                            if(needsElevation){
                                out.add(smallElevation);
                            }
                        }
                    }
                }
            };


            //start actual bfs where we store the edges
            BreadthFirstSearch bfs = new BreadthFirstSearch(){
                @Override
                protected boolean goFurther(int baseNode) {
                    boolean proceed = sector.contains(na.getLat(baseNode), na.getLon(baseNode));
                    if(proceed){
                        nodeProcessor.accept(baseNode);
                    }
                    return proceed;
                }
            };
            bfs.start(edgeExplorer, startNode);

            //DoubleBuffer buf = DoubleBuffer.wrap(out.toArray());
            //DoubleBuffer buf = Buffers.newDirectDoubleBuffer(out.toArray());
            //buf.position(buf.limit());
            //return buf;
            System.out.println(out.size() + " edges found.");

            return (DoubleBuffer) out.getFilledBuffer(false);
        }
        else {
            //No nodes/edges found within sector:
            DoubleBuffer buf = DoubleBuffer.wrap(new double[]{0,0,0,0,0,0});
            buf.position(buf.limit());
            return buf;
        }
    }

    /**
     * Converts a sequence of node (id) visitations into trajectories (for visualisation).
     * @param sequences the integer sequences of node (id) visitations.
     * @return trajectories, which can be visualised.
     */
    public Map<String, SpatialTrajectory> nodeSequencesToTrajectories(int[][] sequences){
        NodeAccess na = gh.getGraphHopperStorage().getNodeAccess();
        Map<String, SpatialTrajectory> trajs = new HashMap<>();

        for (int j = 0; j < sequences.length; j++) {
            int[] seq = sequences[j];
            SpatialTrajectory traj = new SpatialTrajectory();
            for (int nodeId : seq) {
                double lat = na.getLat(nodeId);
                double lon = na.getLon(nodeId);
                traj.addGeographic(new double[]{lat, lon});
            }
            trajs.put(String.valueOf(j), traj);
        }
        return trajs;
    }

    @Override
    public double getLat(int nodeId) {
        return gh.getGraphHopperStorage().getNodeAccess().getLat(nodeId);
    }

    @Override
    public double getLon(int nodeId) {
        return gh.getGraphHopperStorage().getNodeAccess().getLon(nodeId);
    }
}
