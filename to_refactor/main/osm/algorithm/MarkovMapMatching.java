package onethreeseven.trajsuite.osm.algorithm;

import com.carrotsearch.hppc.IntIndexedContainer;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.graphhopper.coll.GHLongObjectHashMap;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.matching.LocationIndexMatch;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.*;
import com.graphhopper.routing.util.*;
import com.graphhopper.routing.weighting.AbstractWeighting;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.SPTEntry;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.*;
import onethreeseven.trajsuite.osm.model.markov.HMMTrellis;
import onethreeseven.trajsuite.osm.model.markov.MarkovState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Performs map matching using hidden markov model (HMM) as an underlying probability model.
 * In this HMM observations are GPS recordings and the hidden states of the model are
 * the road edges. This implementation is based on the work:
 * "Hidden Markov Map Matching Through Noise and Sparseness" by Newson and Krumm.
 * @author Luke Bermingham
 */
public class MarkovMapMatching {

    private static final Logger logger = Logger.getLogger(MarkovMapMatching.class.getSimpleName());
    private final GraphHopperStorage graph;
    private final FlagEncoder flagEncoder;
    private final DistanceCalc distanceCalc;
    private final EdgeFilter edgeFilter;
    private final AbstractWeighting weighting;
    private final EdgeExplorer realExplorer;

    private LocationIndexMatch index;
    private int gpsSearchRadius = 100;
    private int roadNodeId = 0;
    private double sigma;
    private double twoSigma;
    private double oneOverRoot2piSigma;
    private double oneOverBeta;
    private int maxNodesVisited;
    private boolean onlyExpandBest = true;
    private int keepTopN = 10;

    public MarkovMapMatching(GraphHopperStorage graph, LocationIndexTree locationIndex, FlagEncoder flagEncoder) {
        this.graph = graph;
        this.distanceCalc = new DistancePlaneProjection();
        this.flagEncoder = flagEncoder;
        this.edgeFilter = new DefaultEdgeFilter(flagEncoder);
        this.weighting = new ShortestWeighting(flagEncoder);
        this.realExplorer = graph.createEdgeExplorer();
        this.index = new LocationIndexMatch(graph, locationIndex);
        //default params
        setSigma(5);
        setBeta(10);
        setSearchRadius(100);
        setMaxNodesVisited(10);
    }

    /**
     * @param onlyExpandBest Only expanding the best node is far quicker - but far less accurate.
     */
    public void setOnlyExpandBest(boolean onlyExpandBest) {
        this.onlyExpandBest = onlyExpandBest;
    }

    /**
     * If not expanding just the best ({@link #setOnlyExpandBest(boolean)}), how many paths should we keep for expansion.
     * @param keepTopN How many paths to keep in the HMM.
     */
    public void setKeepTopN(int keepTopN) {
        this.keepTopN = keepTopN;
    }

    /**
     * Changes the search radius when we looking for transitioning segments.
     * @param radius How far to look (in meters).
     */
    public void setSearchRadius(int radius){
        this.gpsSearchRadius = radius;
    }

    /**
     * In "Hidden Markov Map Matching Through Noise and Sparseness"
     * sigma represents "standard deviation of Gaussian GPS noise".
     * Note: They also provide a method to estimate this per GPS trail.
     * @param sigma The standard deviation of GPS entries in this trail (metres).
     */
    public void setSigma(double sigma){
        this.sigma = sigma;
        this.twoSigma = sigma * 2;
        this.oneOverRoot2piSigma = 1d/ Math.sqrt(2d * Math.PI * sigma);
    }

    /**
     * In "Hidden Markov Map Matching Through Noise and Sparseness" beta
     * describes the difference between route distances and great circle distances.
     * What they mean by this is median difference between route and great circle
     * distance when considering sequential GPX entries.
     * @param beta The median sequential entry difference (metres).
     */
    public void setBeta(double beta){
        this.oneOverBeta = 1d/beta;
    }

    /**
     * When searching for potential pathsFrom between edges, any pathsFrom that
     * have to skip more than this number of nodes to be connected are not considered.
     * @param maxNodesVisited max number of nodes allowed to skip
     */
    public void setMaxNodesVisited(int maxNodesVisited) {
        this.maxNodesVisited = maxNodesVisited;
    }

    public MatchResult doWork(List<GPXEntry> entries){
        if(entries.isEmpty()){
            throw new IllegalArgumentException("To perform map-matching we must have at least one GPX entry.");
        }

        //Go through each observation and incrementally construct our HMM trellis
        MatchResult mr = new MatchResult(new ArrayList<>());
        subMapMatch(new GPXIterator(entries.iterator()), mr);
        return mr;
    }

    private HMMTrellis constructHMM(GPXIterator iter){
        //go through nodes until we find one which has road edges within our search neighbourhood
        //(very likely to be the first entry we go to unless search neighbourhood is very small)
        while(iter.hasNext()){
            GPXEntry entry = iter.cur;
            if(iter.cur == null){
                iter.next();
                continue;
            }

            List<QueryResult> qrs = index.findNClosest(entry.lat, entry.lon, edgeFilter, gpsSearchRadius);
            if(!qrs.isEmpty()){
                ArrayList<MarkovState> firstStatesList = new ArrayList<>(qrs.size());
                for (QueryResult qr : qrs) {
                    RoadNode node = new RoadNode(new GPXExtension(entry, qr), ++roadNodeId);
                    //check if this state is even possible
                    if(Double.isFinite(node.getLogEmissionPr())){
                        firstStatesList.add(node);
                    }
                }
                //we have some real first states, make trellis
                if(!firstStatesList.isEmpty()){
                    RootState rootState = new RootState();
                    MarkovState[] firstStatesArr = new MarkovState[firstStatesList.size()];
                    firstStatesArr = firstStatesList.toArray(firstStatesArr);
                    return new HMMTrellis(rootState, firstStatesArr, onlyExpandBest, keepTopN);
                }
            }
            //made it here, means we could not find any query results in the neighbourhood,
            //so try the next gpx entry.
            iter.next();
        }
        return null;
    }

    private boolean insert(List<QueryResult> potentialStates, GPXEntry gpxEntry, int gpxEntryIdx, HMMTrellis hmm){
        boolean foundHMMState = false;
        for (QueryResult qr : potentialStates) {
            GPXExtension ext = new GPXExtension(gpxEntry, qr);
            RoadNode roadNode = new RoadNode(ext, ++roadNodeId);
            boolean inserted = hmm.insert(roadNode);
            if(inserted && !foundHMMState){
                foundHMMState = true;
            }
        }
        return foundHMMState;
    }

    private void subMapMatch(GPXIterator iter, MatchResult mr){

        //1) Construct a HMM trellis that we will use to calculate the Viterbi path
        HMMTrellis hmm = constructHMM(iter);
        if(hmm == null){
            //we've reached the end
            if(!iter.hasNext()){
                return;
            }
            throw new IllegalStateException("Cannot proceed, HMM could be constructed resolve the given GPX entries.");
        }

        //Start map-matching process
        GPXEntry lastMatchedEntry = null;
        hmm.setOnlyExtendMostLikely(onlyExpandBest);

        while(iter.hasNext()){
            //set current entry
            GPXEntry curEntry = iter.next();
            if(lastMatchedEntry == null){
                lastMatchedEntry = iter.prev;
            }

            //In "Hidden Markov Map Matching Through Noise and Sparseness"
            //they recommend removing sequential entries that do not move enough
            //because "until we see a point that is at least 2sigma away resolve
            //its temporal predecessor, our confidence is low that the apparent
            //movement is due to actual vehicle movement and not noise".
            double dist = distanceCalc.calcDist(lastMatchedEntry.lat, lastMatchedEntry.lon,
                    curEntry.lat, curEntry.lon);
            if(dist < twoSigma){
                //System.out.println("Too close, skipping:" + iter.getIdx());
                //skip this entry, it is too close to the previous one.
                continue;
            }

            List<QueryResult> potentialStates = index.findNClosest(
                    curEntry.lat, curEntry.lon, edgeFilter, gpsSearchRadius);
            if(potentialStates.isEmpty()){
                //skip this entry, it has no nearby neighbour road edges
                //System.out.println("No nearby road edges, skipping:" + iter.getIdx());
                continue;
            }

            boolean success = insert(potentialStates, curEntry, iter.getIdx(), hmm);
            if(success){
                //System.out.println("Inserted gpx:" + iter.getIdx());
                hmm.moveToNextState();
                lastMatchedEntry = curEntry;
            }
            //Case: no possible states for current GPX entry
            else{
                //logger.info("Going to try making a new HMM trellis at index: " + iter.getIdx() +
                        //" because we cannot find matches at any previous state in the current HMM.");
                //add any existing path into the match result first before doing sub-matching
                viterbiPathToMatchedPath(mr, hmm);
                subMapMatch(iter, mr);
                return;
            }
        }
        //extract the viterbi path
        viterbiPathToMatchedPath(mr, hmm);
    }

    private void viterbiPathToMatchedPath(MatchResult mr, HMMTrellis hmm){
        List<EdgeMatch> edgeMatches = mr.getEdgeMatches();
        EdgeMatch prevEdge = null;
        //todo: if this is being called again (i.e because matching was restarted, make a path between the two matches).

        List<MarkovState> likelyPath = hmm.getViterbiPath();
        RoadNode prev = (RoadNode) likelyPath.get(0);

        for (int i = 1; i < likelyPath.size(); i++) {
            RoadNode cur = (RoadNode) likelyPath.get(i);

            boolean addedExt = false;
            //extract the route resolve the prev node to the current one
            if(cur.pathsFrom.containsKey(prev.getId())){
                Path route = cur.pathsFrom.get(prev.getId());
                //we have a route, extract the real nodes
                if(route != null){
                    IntIndexedContainer nodes = route.calcNodes();

                    for (IntCursor nodeCursor : nodes) {
                        int node = nodeCursor.value;

                        //only want real nodes, discard virtual nodes
                        if (!isVirtualNode(node)) {
                            boolean stayedStill = prevEdge != null && node == prevEdge.getEdgeState().getBaseNode();
                            //we have a new (different resolve previous) real node, initialise the edge
                            if (prevEdge != null && !stayedStill) {
                                boolean foundEdge = false;
                                EdgeIterator prevEdgeIter = (EdgeIterator) prevEdge.getEdgeState();
                                while (prevEdgeIter.next()) {
                                    if (prevEdgeIter.getAdjNode() == node) {
                                        //we have made a real edge
                                        foundEdge = true;
                                        int idx = edgeMatches.size() - 1;
                                        //overwrite last edge match with a cloned edge state
                                        edgeMatches.set(idx, new EdgeMatch(prevEdge.getEdgeState().detach(false),
                                                edgeMatches.get(idx).getGpxExtensions()));
                                        break;
                                    }
                                }
                                if (!foundEdge) {
                                    throw new IllegalStateException("Could not find edge between real node: "
                                            + node + " and: " + prevEdge.getEdgeState().getBaseNode());
                                }
                            }
                            //stayed still, just store the GPX extension
                            if (stayedStill && !addedExt) {
                                prevEdge.getGpxExtensions().add(cur.gpxExtension);
                                addedExt = true;
                            }
                            //moved to a different real node
                            else {
                                //add the current node as the previous uninitialised edge
                                ArrayList<GPXExtension> exts = new ArrayList<>();
                                if (!addedExt) {
                                    exts.add(cur.gpxExtension);
                                    addedExt = true;
                                }
                                EdgeMatch match = new EdgeMatch(realExplorer.setBaseNode(node), exts);
                                edgeMatches.add(match);
                                prevEdge = match;
                            }
                        }

                    }
                }
                //we stayed still, just store the GPX extension
                if(prevEdge != null && !addedExt){
                    prevEdge.getGpxExtensions().add(cur.gpxExtension);
                }
            }
            prev = cur;
        }

        //remove the last edge match because it will be uninitialised
        if(!edgeMatches.isEmpty()){
            edgeMatches.remove(edgeMatches.size()-1);
        }
    }

    private boolean isVirtualNode(int nodeId){
        return nodeId >= graph.getNodes();
    }

    /**
     * Following the literature we can model each state in the hidden markov model
     * as nodes in the road network. No need to model all nodes, only the ones
     * close to GPX entries.
     */
    private class RoadNode extends MarkovState {

        final GPXExtension gpxExtension;
        final GHLongObjectHashMap<Path> pathsFrom;
        final long id;

        RoadNode(GPXExtension gpxExtension, int id) {
            this.id = id;
            this.gpxExtension = gpxExtension;
            this.pathsFrom = new GHLongObjectHashMap<>();
        }

        public long getId(){
            return id;
        }

        /**
         * @return The probability that this road segment is a match for a specific GPX entry.
         */
        @Override
        public double getLogEmissionPr() {
            //see Eq(1) in "Hidden Markov Map Matching Through Noise and Sparseness"
            double exp = -0.5d * Math.pow(gpxExtension.getQueryResult().getQueryDistance()/sigma, 2);
            return Math.log(oneOverRoot2piSigma * (Math.exp(exp)));
        }

        /**
         * @param toState The other road node to go to.
         * @return The probability of the GPX trail moving resolve this road node to some other.
         */
        @Override
        public double getLogTransitionPr(MarkovState toState) {
            RoadNode toNode = (RoadNode) toState;
            QueryResult from = cloneQr(this.gpxExtension.getQueryResult());
            QueryResult to = cloneQr(toNode.gpxExtension.getQueryResult());

            double dist = distanceCalc.calcDist(
                    from.getQueryPoint().lat, from.getQueryPoint().lon,
                    to.getQueryPoint().lat, to.getQueryPoint().lon);

            double routeDistance = 0;
            //note: lookup() has to be outside the if/else because it can
            //change the closest nodes
            QueryGraph g = new QueryGraph(graph);
            g = g.lookup(from, to);

            //do routing (if we aren't on the same node)
            if(from.getClosestNode() != to.getClosestNode()){

                //check if there is direct path between neighbours
                //Path path = checkForNeighbourPath(g, resolve.getClosestNode(), to.getClosestNode());
                //if not, check for a path between nodes
                //if(path == null){
                    AStar algo = new AStar(g, weighting, TraversalMode.NODE_BASED);
                    algo.setMaxVisitedNodes(maxNodesVisited);
                    Path path = algo.calcPath(from.getClosestNode(), to.getClosestNode());
                //}

                routeDistance = path.getDistance();

                //case: weight limit exceeded
                if(routeDistance == 0){
                    return Double.NEGATIVE_INFINITY;
                }

                //store path for when we are constructing match result
                toNode.pathsFrom.put(getId(), path);
            }
            else{
                //we set null as a marker that there was no transition
                toNode.pathsFrom.put(getId(), null);
            }
            return Math.log(oneOverBeta * Math.exp(oneOverBeta * -Math.abs(dist - routeDistance)));
        }

        private Path checkForNeighbourPath(QueryGraph g, int from, int to){
            //check if they are neighbours
            EdgeExplorer qgEdgeExplorer = g.createEdgeExplorer();
            EdgeIterator iter = qgEdgeExplorer.setBaseNode(from);
            while(iter.next()){
                if(iter.getAdjNode() == to){

                    SPTEntry goalEdge;
                    //make sure it is forward or else reverse it
                    if(!iter.isForward(flagEncoder)){
                        EdgeIteratorState edgeIteratorState = g.getEdgeIteratorState(iter.getEdge(), iter.getBaseNode());
                        SPTEntry rootEdge = new SPTEntry(EdgeIterator.NO_EDGE, to, 0);
                        goalEdge = new SPTEntry(edgeIteratorState.getEdge(), from, iter.getDistance());
                        goalEdge.parent = rootEdge;
                    }else{
                        SPTEntry rootEdge = new SPTEntry(EdgeIterator.NO_EDGE, from, 0);
                        goalEdge = new SPTEntry(iter.getEdge(), to, iter.getDistance());
                        goalEdge.parent = rootEdge;
                    }

                    Path path = new Path(g, weighting);
                    path.setSPTEntry(goalEdge);
                    return path.extract();
                }
            }
            return null;
        }

        private QueryResult cloneQr(QueryResult qr){
            QueryResult copy = new QueryResult(qr.getQueryPoint().lat, qr.getQueryPoint().lon);
            copy.setSnappedPosition(qr.getSnappedPosition());
            copy.setQueryDistance(qr.getQueryDistance());
            copy.setClosestEdge(qr.getClosestEdge());
            copy.setClosestNode(qr.getClosestNode());
            copy.setWayIndex(qr.getWayIndex());
            copy.calcSnappedPoint(distanceCalc);
            return copy;
        }

    }

    /**
     * This is a special case to make the HMM work.
     * Basically we set the transition probabilities to
     * the emission probabilities for the root node
     * (because it has no transition probability)
     */
    private class RootState extends MarkovState {
        @Override
        public double getLogEmissionPr() {
            //no such thing for the root node
            return 0;
        }

        @Override
        public double getLogTransitionPr(MarkovState toState) {
            return toState.getLogEmissionPr();
        }
    }

    /**
     * A wrapper around the iterator for GPX Entries, it adds the additional
     * functionality of tracking the index.
     */
    private class GPXIterator implements Iterator<GPXEntry>{

        private final Iterator<GPXEntry> realIter;
        private int idx = -1;
        private GPXEntry cur;
        private GPXEntry prev;

        private GPXIterator(Iterator<GPXEntry> realIter){
            this.realIter = realIter;
        }

        @Override
        public boolean hasNext() {
            return realIter.hasNext();
        }

        @Override
        public GPXEntry next() {
            //update the previous entry
            prev = cur;
            //set the new current entry
            cur = realIter.next();
            idx++;
            return cur;
        }

        @Override
        public void remove() {
            realIter.remove();
        }

        @Override
        public void forEachRemaining(Consumer<? super GPXEntry> action) {
            realIter.forEachRemaining(action);
        }

        int getIdx() {
            return idx;
        }
    }

}
