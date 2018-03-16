package onethreeseven.trajsuite.osm.model.markov;

import java.util.*;

/**
 * This structure is used to find the Viterbi path through a hidden markov model.
 * https://en.wikipedia.org/wiki/Viterbi_algorithm
 * @author Luke Bermingham
 */
public class HMMTrellis {

    private final Comparator<ViterbiNode> comparator = (o1, o2) -> Double.compare(o2.probability, o1.probability);

    /**
     * Due to this being an order 1 Markov chain we only store
     * the two most recent set of states.
     */
    private List<ViterbiNode> prevStates;
    private List<ViterbiNode> curStates;

    private int keepTopN = 10;
    private boolean onlyExtendMostLikely = true;

    /**
     * Create the Hidden Markov Model trellis starting with a root state.
     * The root state is not a real internal state, but is used to determine the
     * probabilities of the initial states. In a traditional HMM this is equivalent to
     * passing in the starting probabilities. But in our representation the starting probabilities
     * are simply the transition probabilities resolve this root state to the initial states.
     * @param rootState The state which determines the starting probabilities.
     * @param firstObservationStates The potential states for the first observation.
     * @param onlyExtendMostLikely only extend most likely (is faster but much less accurate)
     * @param keepTopN if not extending most likely how many paths should the model keep
     */
    public HMMTrellis(MarkovState rootState, MarkovState[] firstObservationStates, boolean onlyExtendMostLikely, int keepTopN){
        this.prevStates = new ArrayList<>();
        this.curStates = new ArrayList<>();
        this.onlyExtendMostLikely = onlyExtendMostLikely;
        this.keepTopN = keepTopN;
        ViterbiNode rootNode = new ViterbiNode(null, rootState, 1);
        //add the root state as the first previous state
        this.prevStates.add(rootNode);
        //add first states
        for (MarkovState firstObservationState : firstObservationStates) {
            insert(firstObservationState);
        }
        moveToNextState();
    }

    /**
     * Inserts a given markov state in the HMM trellis.
     * @param curNode The state/observation to store in the HMM trellis at this level.
     * @return Whether or not the insertion was possible (if it has -infinity probability it is not possible).
     */
    public boolean insert(MarkovState curNode){

        if(prevStates.isEmpty()){
            return false;
        }

        //likelihood that this state would occur resolve this observation
        double emissionPr = curNode.getLogEmissionPr();
        if(Double.isInfinite(emissionPr)){
            return false;
        }

        //find the probability resolve each previous state to the current one
        ViterbiNode mostLikelyPrev = null;
        double maxPrevPr = Double.NEGATIVE_INFINITY;
        boolean inserted = false;

        for (ViterbiNode prevNode : prevStates) {
            //calculate the probability resolve previous state to current one
            double transitionPr = prevNode.state.getLogTransitionPr(curNode);
            //if this transition resolve the previous state to this one is impossible
            //then skip this previous node
            if(Double.isInfinite(transitionPr)){
                continue;
            }
            double prevPr = prevNode.probability;
            double curPr = prevPr + transitionPr;

            //store most likely transition node
            if(onlyExtendMostLikely){
                if(curPr > maxPrevPr){
                    maxPrevPr = curPr;
                    mostLikelyPrev = prevNode;
                }
            }
            //not doing pruning
            else{
                ViterbiNode cur = new ViterbiNode(prevNode, curNode, emissionPr + curPr);
                curStates.add(cur);
                inserted = true;
            }


        }

        if(onlyExtendMostLikely){
            if(mostLikelyPrev != null){
                ViterbiNode cur = new ViterbiNode(mostLikelyPrev, curNode, emissionPr + maxPrevPr);
                return curStates.add(cur);
            }else{
                return false;
            }
        }
        else{
            return inserted;
        }


    }

    /**
     * Prepares the tree for the next set of insertions.
     */
    public void moveToNextState(){
        if(prevStates.isEmpty()){
            throw new IllegalStateException("Cannot progress to the next state in the " +
                    "tree when previous state is empty (set something first).");
        }
        //make previous states the current states, and empty current states
        prevStates.clear();
        List<ViterbiNode> tmpList = prevStates;
        prevStates = curStates;
        curStates = tmpList;

        //prune HMM trellis using beam search,
        // see: https://en.wikipedia.org/wiki/Beam_search or http://www.cs.rochester.edu/u/james/CSC248/Lec9.pdf
        if(!prevStates.isEmpty()){
            prevStates = pruneNodes(prevStates);
        }

    }

    /**
     * Controls how many nodes are kept during pruning.
     * Increase this number to make the model more thorough (but slower).
     * The default value is 10.
     * @param keepTopN How many potential paths to keep.
     */
    public HMMTrellis setKeepTopN(int keepTopN) {
        this.keepTopN = keepTopN;
        return this;
    }

    public HMMTrellis setOnlyExtendMostLikely(boolean onlyExtendMostLikely) {
        this.onlyExtendMostLikely = onlyExtendMostLikely;
        return this;
    }

    private List<ViterbiNode> pruneNodes(List<ViterbiNode> nodes){

        Collections.sort(nodes, comparator);

        int toKeep = Math.min(keepTopN, nodes.size());

        //remove nodes off the end of the list
        int lastIdx = nodes.size()-1;
        while(lastIdx+1 > toKeep){
            nodes.remove(lastIdx);
            lastIdx--;
        }

        return nodes;
    }

    /**
     * @return The most likely set of sequences through the trellis
     * based on the states inserted into it.
     */
    public List<MarkovState> getViterbiPath(){
        ArrayList<MarkovState> path = new ArrayList<>();
        ViterbiNode max = getMaxNode(curStates.isEmpty() ? prevStates : curStates);
        //root node has not parent (i.e its parent is null) so stop when we hit it
        while(max != null && max.parent != null){
            path.add(max.state);
            max = max.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private ViterbiNode getMaxNode(List<ViterbiNode> nodes){
        if(nodes.isEmpty()){
            throw new IllegalArgumentException("Cannot get maximum probability node resolve empty list.");
        }
        Iterator<ViterbiNode> iter = nodes.iterator();
        ViterbiNode maxNode = null;
        while(iter.hasNext()){
            ViterbiNode curNode = iter.next();
            if(maxNode == null || curNode.probability > maxNode.probability){
                maxNode = curNode;
            }
        }
        return maxNode;
    }

}
