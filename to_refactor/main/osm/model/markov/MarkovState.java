package onethreeseven.trajsuite.osm.model.markov;

/**
 * A state in a markov model, these model the internal finite states that the model can
 * potentially be in at any given observation. For our purposes we model states as unique,
 * and thus we give each state its own id. Also, we enforce that each state can calculate
 * the probability of itself occurring (i.e its emission probability) and  the probability
 * of moving itself to some other states (i.e its transition probabilities).
 * However, these probabilities are problem specific, and are therefore left abstract.
 * @author Luke Bermingham
 */
public abstract class MarkovState {

    /**
     * Calculates the log-probability that this state occurred given some observation.
     * @return The emission probability (higher number means it is more likely).
     */
    public abstract double getLogEmissionPr();

    /**
     * Calculate the log-probability that this state will transition to
     * some other state.
     * @param toState The state being transitioned to.
     * @return The transition probability (higher number means it is more likely).
     */
    public abstract double getLogTransitionPr(MarkovState toState);

}
