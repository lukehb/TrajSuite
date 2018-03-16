package onethreeseven.trajsuite.osm.model.markov;

import com.carrotsearch.hppc.cursors.IntCursor;
import com.graphhopper.coll.GHIntHashSet;
import com.graphhopper.routing.util.*;
import com.graphhopper.storage.*;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Testing making graph manually.
 * @author Luke Bermingham
 */
public class GraphHopperManualGraphTest {

    private static GraphHopperStorage graph;
    private static LocationIndexTree index;
    private static EdgeFilter edgeFilter;
    private static final int nPts = 10;

    private static final double minLat = 10;
    private static final double minLon = 10;
    private static final double maxLat = 10.1;
    private static final double maxLon = 10.1;

    @BeforeClass
    public static void setup(){

        FlagEncoder flagEncoder = new CarFlagEncoder();
        EncodingManager carManager = new EncodingManager(flagEncoder);
        edgeFilter = new DefaultEdgeFilter(flagEncoder);

        //make gridded graph
        graph = new GraphBuilder(carManager).create();
        createGridGraph(graph, minLat, minLon, maxLat, maxLon, nPts);
        index = new LocationIndexTree(graph, new RAMDirectory());
        //index = new LocationIndexMatch(graph, locationIndexTree);
        index.prepareIndex();

    }

    private static void createGridGraph(Graph g, double lowerLeftLat, double lowerLeftLon, double upperRightLat, double upperRightLon, int nPts){

        final double totalLat = upperRightLat - lowerLeftLat;
        final double totalLon = upperRightLon - lowerLeftLon;
        final double deltaLat = totalLat / nPts;
        final double deltaLon = totalLon / nPts;
        final NodeAccess na = g.getNodeAccess();
        final double dist = 1;

        //make all the nodes
        int a = 0;
        for (int row = 0; row < nPts; row++) {
            for (int col = 0; col < nPts; col++) {
                double lat = lowerLeftLat + (row * deltaLat);
                double lon = lowerLeftLon + (col * deltaLon);
                na.setNode(a, lat, lon);
                a++;
            }
        }

        //connect all the nodes with edges
        //connect in the 4 cardinal directions
        a = 0;
        for (int row = 0; row < nPts; row++) {
            for (int col = 0; col < nPts; col++) {
                //left
                if(col - 1 >= 0){
                    int b = a - 1;
                    g.edge(a, b , dist, false).setName(a + "->" + b);
                }
                //right
                if(col + 1 <= nPts - 1){
                    int b = a + 1;
                    g.edge(a, b, dist, false).setName(a + "->" + b);
                }
                //up
                if(row + 1 <= nPts - 1){
                    int b = a + nPts;
                    g.edge(a, b, dist, false).setName(a + "->" + b);
                }
                //down
                if(row - 1 >= 0){
                    int b = a - nPts;
                    g.edge(a, b, dist, false).setName(a + "->" + b);
                }
                a++;
            }
        }
    }

    @Test
    public void testMakeGridGraph() throws Exception {
        int totalPts = nPts * nPts;
        EdgeExplorer ee = graph.createEdgeExplorer(edgeFilter);
        for (int i = 0; i < totalPts; i++) {
            EdgeIterator edgeIter = ee.setBaseNode(i);
            int count = 1;
            System.out.println("------------------------------------");
            System.out.println("        At node: " + i);
            while(edgeIter.next()){
                System.out.println("Got edge: " + edgeIter.getName());
                count++;
            }
            System.out.println("------------------------------------");
            Assert.assertTrue(count >= 4);
        }
    }

    @Test
    public void testFindNearest(){

        double queryLat = 10.055;
        double queryLon = 10.045;

        //QueryResult qr = index.findClosest(midLat, midLon, edgeFilter);

        GHIntHashSet neighbourIds = new GHIntHashSet();

        boolean isSearching = true;
        int i = 0;
        while(isSearching){
            isSearching = !index.findNetworkEntries(queryLat, queryLon, neighbourIds, i);
            i++;
        }

        for (IntCursor neighbourId : neighbourIds) {
            System.out.println("\n Neighbour id: " + neighbourId);
        }

        Assert.assertTrue(neighbourIds.size() >= 1);

    }
}