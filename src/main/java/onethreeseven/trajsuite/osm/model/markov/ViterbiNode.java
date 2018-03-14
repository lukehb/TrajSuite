package onethreeseven.trajsuite.osm.model.markov;

/**
 * A node in the {@link HMMTrellis}.
 * The node is stored at a certain level (t) within the trellis.
 * It also has a probability associated with it for being at that level in the trellis.
 * @author Luke Bermingham
 */
class ViterbiNode {

    final MarkovState state;
    final double probability;
    final ViterbiNode parent;

    ViterbiNode(ViterbiNode parent, MarkovState state, double probability) {
        this.parent = parent;
        this.state = state;
        this.probability = probability;
    }
}
