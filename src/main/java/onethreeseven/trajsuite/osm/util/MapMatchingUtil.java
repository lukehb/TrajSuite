package onethreeseven.trajsuite.osm.util;

import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.MatchResult;
import onethreeseven.trajsuite.osm.model.GraphHopperDAO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility used when doing Map Matching.
 * @author Luke Bermingham
 */
public final class MapMatchingUtil {

    private MapMatchingUtil(){}

    /**
     * Converts a Match Result to a trajectory of node visits with one long entry in it (a sequence).
     * @param mr The match result
     * @return A 1D sequence.
     */
    public static int[] toSequence(MatchResult mr){

        int[] nodeVisits = new int[mr.getEdgeMatches().size() + 1];
        List<EdgeMatch> ems = mr.getEdgeMatches();
        if(ems.size() == 0){
            return new int[]{};
        }

        nodeVisits[0] = ems.get(0).getEdgeState().getBaseNode();
        for (int i = 0; i < ems.size(); i++) {
            nodeVisits[i+1] = ems.get(i).getEdgeState().getAdjNode();
        }
        return nodeVisits;
    }

    /**
     * Convert a map-matched result to extract all unique node id with their geographic coordinates (lat/lon).
     * @param mr The match result.
     * @param dao To access the underlying graph of nodes.
     * @return The map of ids to geographic coordinates.
     */
    public static Map<Integer, double[]> getIdToPositionMap(MatchResult mr, GraphHopperDAO dao){

        HashMap<Integer, double[]> out = new HashMap<>();

        List<EdgeMatch> ems = mr.getEdgeMatches();
        if(ems.size() == 0){
            return out;
        }

        {
            int baseNode = ems.get(0).getEdgeState().getBaseNode();
            double[] baseNodeLL = new double[]{dao.getLat(baseNode), dao.getLon(baseNode)};
            out.put(baseNode, baseNodeLL);
        }

        for (EdgeMatch em : ems) {
            int adjNode = em.getEdgeState().getAdjNode();
            double[] adjNodeLL = new double[]{dao.getLat(adjNode), dao.getLon(adjNode)};
            out.put(adjNode, adjNodeLL);
        }
        return out;

    }

}
