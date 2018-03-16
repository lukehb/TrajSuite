package onethreeseven.trajsuite.osm.algorithm;

import onethreeseven.datastructures.model.CompositePt;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.trajsuite.core.model.Tuple;
import onethreeseven.trajsuite.osm.model.*;
import onethreeseven.trajsuite.osm.model.markov.HMMTrellis;
import onethreeseven.trajsuite.osm.model.markov.MarkovState;
import onethreeseven.trajsuite.osm.model.tag.OsmTag;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Similar to "map-matching" but for places.
 * Matches a given stop region sequence to the potential places it most likely visited.
 * @author Luke Bermingham
 */
public class HMMPlaceMatching implements IPlaceMatching {

    private static final Logger log = Logger.getLogger(HMMPlaceMatching.class.getSimpleName());

    private double homogeneityFactor = 0.5;
    private int keepTopN = 10;
    private double maxSearchRadius = 200;
    private boolean queryWebForBetterPlaces = true;
    private boolean onlyExpandBest = true;

    public HMMPlaceMatching(double maxSearchRadius){
        this.maxSearchRadius = maxSearchRadius;
    }

    @Override
    public Map<String, SemanticTrajectory> run(Map<String, ArrayList<SemanticTrajectoryBuilder.StopRegion>> trajs, AbstractGeographicProjection projection){

        //first, analyse the trajectories to build the initial state probabilities and transition matrix
        final Table<String, Double> transitionMatrix;
        final RootState rootState;
        {
            log.info("Learning transition matrix for data-set.");
            Tuple<Table<String, Double>, Map<String, Double>> learntPrs = learnProbabilities(trajs, homogeneityFactor);
            transitionMatrix = learntPrs.getValue1();
            rootState = new RootState(learntPrs.getValue2());
        }

        final Map<String, SemanticTrajectory> output = new HashMap<>();


        //go through each trajectory and do place-matching using a HMM
        for (Map.Entry<String, ArrayList<SemanticTrajectoryBuilder.StopRegion>> entry : trajs.entrySet()) {
            log.info("Building HMM for sequence: " + entry.getKey());
            HMMTrellis hmm = populateHMM(entry.getValue(), rootState, transitionMatrix);

            if(hmm == null){
                continue;
            }

            log.info("Extracting likely path for sequence: " + entry.getKey());
            //extract the most likely set of places from the hmm and make a trajectory
            List<MarkovState> proposedPath = hmm.getViterbiPath();
            if(proposedPath != null && !proposedPath.isEmpty()){
                SemanticTrajectory traj = new SemanticTrajectory(true, projection);

                for (MarkovState state : proposedPath) {
                    PlaceNode curNode = (PlaceNode) state;


                    TimeAndPlace timeAndPlace = new TimeAndPlace(
                            curNode.observedStop.getTime(),
                            curNode.observedStop.getTimeCategory(),
                            curNode.place.getSemantics());
                    traj.addCartesian(curNode.observedStop.getCoords(), timeAndPlace);
                }
                output.put(entry.getKey(), traj);
            }
        }

        //enrich found places with foursquare
        if(queryWebForBetterPlaces){
            //log.info("Enriching ambiguous places with more information from FourSquare.");
            //enrichFoundPlaces(output);
        }

        return output;
    }

//    private void enrichFoundPlaces(Map<String, SemanticTrajectory> trajs){
//
//        //any tag that has low specificity gets a query to FourSquare for a better place data
//        final OsmTag.Specificity minAllowedSpecificity = OsmTag.Specificity.GENERIC;
//        final FourSquarePlaceService service = new FourSquarePlaceService();
//
//        //internal class used for querying foursquare and updating the places
//        class QueryFourSquare implements Runnable{
//            private SemanticPt pt;
//            private QueryFourSquare(SemanticPt pt){
//                this.pt = pt;
//            }
//
//            @Override
//            public void run(){
//                CompositePt<SemanticPlace> result = service.getClosestPlace(pt.getCoords()[0], pt.getCoords()[1], (int) maxSearchRadius);
//                SemanticPlace place = result.getExtra();
//                if(place != null && place.getSpecificity() >= minAllowedSpecificity.level){
//                    this.pt.getTimeAndPlace().setPlace(place);
//                }
//            }
//        }
//
//        final ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//
//        for (Map.Entry<String, SemanticTrajectory> entry : trajs.entrySet()) {
//
//            //make sure we are in geographic mode
//            entry.getValue().toGeographic();
//
//            for (SemanticPt semanticPt : entry.getValue()) {
//                SemanticPlace place = semanticPt.getTimeAndPlace().getPlace();
//                OsmTag.Specificity specifity = place.getPrimaryTag().getSpecificity();
//                if(specifity.level < minAllowedSpecificity.level){
//                    exec.submit(new QueryFourSquare(semanticPt));
//                }
//            }
//        }
//
//        //process the jobs
//        try {
//            exec.shutdown();
//            exec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
//        } catch (SecurityException | InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    private HMMTrellis populateHMM(ArrayList<SemanticTrajectoryBuilder.StopRegion> traj,
                                   RootState rootState, Table<String, Double> transitionMatrix){

        Iterator<SemanticTrajectoryBuilder.StopRegion> iter = traj.iterator();
        //createAnnotation HMM
        HMMTrellis hmm = null;

        while(iter.hasNext()){
            PlaceNode[] firstNodes = getNextPlaceNodes(iter.next(), transitionMatrix);
            if(firstNodes.length <= 0){
                continue;
            }
            hmm = new HMMTrellis(rootState, firstNodes, onlyExpandBest, keepTopN);
            break;
        }

        if(hmm == null){
            return null;
        }

        //populate trellis
        while(iter.hasNext()){
            PlaceNode[] potentialNextPlaces = getNextPlaceNodes(iter.next(), transitionMatrix);
            if(potentialNextPlaces.length <= 0){continue;}
            boolean didInsertAState = false;

            for (PlaceNode placeNode : potentialNextPlaces) {
                boolean inserted = hmm.insert(placeNode);
                if(!didInsertAState && inserted){
                    didInsertAState = true;
                }
            }

            if(!didInsertAState){
                throw new UnsupportedOperationException("Healing HMM breaks not yet implemented.");
            }

            hmm.moveToNextState();
        }
        return hmm;
    }

    /**
     * Learns the initial starting probabilities and transition probabilities from the available data.
     * This is done for the initial prs by observing how many times each type of place is possibly visited
     * in the data-set.
     * Likewise for the transition prs this is done by observing how many times each type of places potentially
     * transitions to other places (of the same type of different - doesn't matter).
     * @param trajs The trajectories, which are sequences of potential places being visited at each entry.
     * @param homogeneityFactor A value of 0 means they entities move completely randomly, and a value of 1 means
     *                          they have the exact same habits.
     * @return The initial prs and the transition prs normalised and logged (i.e the log-likelihood).
     */
    private Tuple<Table<String, Double>, Map<String, Double>> learnProbabilities(Map<String, ArrayList<SemanticTrajectoryBuilder.StopRegion>> trajs, double homogeneityFactor){

        //total occurrences of each tag
        int totalPlaces = 0;
        HashMap<String, Double> totalOccurences = new HashMap<>();
        //total transitional occurrences between tags
        Table<String, Double> allTransitions = new Table<>();
        Map<String, Integer> totalTransitionFromTag = new HashMap<>();

        //go through the trajs and learn the total occurrences and total transitions for each tag
        for (ArrayList<SemanticTrajectoryBuilder.StopRegion> traj : trajs.values()) {
            SemanticTrajectoryBuilder.StopRegion prev = null;
            for (SemanticTrajectoryBuilder.StopRegion cur : traj) {
                //go through tags
                for (SemanticTrajectoryBuilder.CandidatePlace candidatePlace : cur.getPlaces()) {
                    String curPlaceType = getPlaceType(candidatePlace.getSemantics().getPrimaryTag());
                    //add to total occurrences
                    totalOccurences.put(curPlaceType, totalOccurences.getOrDefault(curPlaceType, 0d) + candidatePlace.getProbability());
                    totalPlaces++;
                    //add to transitions (if we have a previous stop region)
                    if(prev != null){
                        for (SemanticTrajectoryBuilder.CandidatePlace prevCandidatePlace : prev.getPlaces()) {
                            String prevPlaceType = getPlaceType(prevCandidatePlace.getSemantics().getPrimaryTag());

                            totalTransitionFromTag.put(prevPlaceType, totalTransitionFromTag.getOrDefault(prevPlaceType, 0)+1);

                            Double transitionCount = allTransitions.getOrDefault(prevPlaceType, curPlaceType, 0d) + candidatePlace.getProbability();
                            allTransitions.put(prevPlaceType, curPlaceType, transitionCount);
                        }
                    }
                    prev = cur;
                }
            }
        }

        //now normalise and log-likelihood all the probabilities
        for (Map.Entry<String, Double> tagOccurrenceEntry : totalOccurences.entrySet()) {
            Double logLikelihood = tagOccurrenceEntry.getValue() / totalPlaces * homogeneityFactor;
            tagOccurrenceEntry.setValue(logLikelihood);
        }

        //now do the same thing for the transition matrix
        for (String tag : totalOccurences.keySet()) {
            Map<String, Double> transitionsFromTag = allTransitions.getRow(tag);
            Integer totalTransitionsFromTag = totalTransitionFromTag.get(tag);
            for (Map.Entry<String, Double> transitionEntry : transitionsFromTag.entrySet()) {
                Double logLikelihood = transitionEntry.getValue()/totalTransitionsFromTag * homogeneityFactor;
                transitionEntry.setValue(logLikelihood);
            }
        }
        return new Tuple<>(allTransitions, totalOccurences);
    }

    /**
     * @param homogeneityFactor A value of 0 means completely random and divergent behaviours
     *                          and a value of 1 means completely group-like behaviours.
     * @return A reference to this object, for method chaining.
     */
    public HMMPlaceMatching setHomogeneityFactor(double homogeneityFactor) {
        this.homogeneityFactor = homogeneityFactor;
        return this;
    }

    public HMMPlaceMatching setKeepTopN(int keepTopN) {
        this.keepTopN = keepTopN;
        return this;
    }

    private PlaceNode[] getNextPlaceNodes(SemanticTrajectoryBuilder.StopRegion stopRegion, Table<String, Double> transitionMatrix){

        ArrayList<SemanticTrajectoryBuilder.CandidatePlace> candidatePlaces = stopRegion.getPlaces();
        PlaceNode[] nodes = new PlaceNode[candidatePlaces.size()];

        for (int i = 0; i < candidatePlaces.size(); i++) {
            SemanticTrajectoryBuilder.CandidatePlace candidatePlace = candidatePlaces.get(i);
            nodes[i] = new PlaceNode(stopRegion.getStopEpisode(), candidatePlace, transitionMatrix);
        }

        return nodes;
    }

    private String getPlaceType(OsmTag tag){
        return tag.getName();
    }

    public HMMPlaceMatching setQueryWebForBetterPlaces(boolean queryWebForBetterPlaces) {
        this.queryWebForBetterPlaces = queryWebForBetterPlaces;
        return this;
    }

    public HMMPlaceMatching setOnlyExpandBest(boolean onlyExpandBest) {
        this.onlyExpandBest = onlyExpandBest;
        return this;
    }

    /**
     * This is a special case to make the HMM work.
     * Basically we set the transition probabilities to
     * the emission probabilities for the root node
     * (because it has no transition probability)
     */
    private class RootState extends MarkovState {

        private final Map<String, Double> initialPrs;

        RootState(Map<String, Double> intialPrs) {
            this.initialPrs = intialPrs;
        }

        @Override
        public double getLogEmissionPr() {
            //no such thing for the root node
            return 0;
        }

        @Override
        public double getLogTransitionPr(MarkovState toState) {
            //return 0;
            if(toState instanceof PlaceNode){
                String placeType = getPlaceType(((PlaceNode) toState).place.getSemantics().getPrimaryTag());
                return initialPrs.get(placeType);
            }
            throw new IllegalArgumentException("The \"To state\" must be of type PlaceNode.");
        }
    }

    private class PlaceNode extends MarkovState{

        private final StopEpisode observedStop;
        private final SemanticTrajectoryBuilder.CandidatePlace place;
        private final Table<String, Double> transitionMatrix;

        PlaceNode(StopEpisode observedStop,
                  SemanticTrajectoryBuilder.CandidatePlace place,
                  Table<String, Double> transitionMatrix) {
            this.transitionMatrix = transitionMatrix;
            this.observedStop = observedStop;
            this.place = place;
        }

        @Override
        public double getLogEmissionPr() {
            return place.getProbability();
        }

        @Override
        public double getLogTransitionPr(MarkovState toState){
            if(toState instanceof PlaceNode){
                String from = getPlaceType(this.place.getSemantics().getPrimaryTag());
                String to = getPlaceType(((PlaceNode) toState).place.getSemantics().getPrimaryTag());
                Double transitionPr = transitionMatrix.get(from, to);
                if(transitionPr == null){
                    return Math.log(1e-7);
                }
                return transitionPr;
            }
            throw new IllegalArgumentException("To state must be of type PlaceNode.");
        }
    }

}
