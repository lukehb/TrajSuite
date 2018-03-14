package onethreeseven.trajsuite.osm.algorithm;

import onethreeseven.trajsuite.osm.model.SemanticPlace;
import onethreeseven.trajsuite.osm.model.SemanticPt;
import onethreeseven.trajsuite.osm.model.SemanticTrajectory;
import onethreeseven.trajsuite.osm.model.tag.Building;
import onethreeseven.trajsuite.osm.model.tag.Landuse;
import onethreeseven.trajsuite.osm.model.tag.OsmTag;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Given a semantic trajectory annotate likely places as the users home.
 * @author Luke Bermingham
 */
public class HomeEstimator {

    private final PlaceEvaluator placeEvaluator;

    public HomeEstimator(){
        this.placeEvaluator = new HumanHomeEvaluator();
    }

    public HomeEstimator(PlaceEvaluator placeEvaluator){
        this.placeEvaluator = placeEvaluator;
    }

    public void run(SemanticTrajectory trajectory){
        //<PlaceId, LogLikelihood>
        HashMap<String, Double> homeProbabilities = new HashMap<>();

        //store the home likelihood for each place in this trajectory
        for (SemanticPt semanticPt : trajectory) {
            double pr = placeEvaluator.getHomeProbability(semanticPt);
            if(pr > 0){
                double logLikelihood = Math.log(pr);
                String placeId = semanticPt.getTimeAndPlace().getPlace().getId();
                homeProbabilities.put(placeId, homeProbabilities.getOrDefault(placeId, 0d) + logLikelihood);
            }
        }

        //find the placeId with the highest probability
        Optional<Map.Entry<String, Double>> mostLikely =
                homeProbabilities.entrySet().stream().max((o1, o2) -> Double.compare(o1.getValue(), o2.getValue()));

        //if there is a most likely home go through the trajectory and update this place accordingly as a home
        if(mostLikely.isPresent()){
            Map.Entry<String, Double> mostLikelyHome = mostLikely.get();
            String placeId = mostLikelyHome.getKey();

            final SemanticPlace home = new SemanticPlace(placeId, "Home", new Building("house"));

            for (SemanticPt semanticPt : trajectory) {
                if(semanticPt.getTimeAndPlace().getPlace().getId().equals(placeId)){
                    semanticPt.getTimeAndPlace().setPlace(home);
                }
            }

        }

    }

    public interface PlaceEvaluator{
        /**
         * @param pt The place the evaluate.
         * @return The probability that this place is the entity's home.
         * A value of 1 indicates certainty that this is the entity's home
         * and a value of 0 indicates no likelihood of inhabitance.
         */
        double getHomeProbability(SemanticPt pt);
    }

    private class HumanHomeEvaluator implements PlaceEvaluator{
        @Override
        public double getHomeProbability(SemanticPt pt) {
            SemanticPlace place = pt.getTimeAndPlace().getPlace();
            OsmTag tag = place.getPrimaryTag();

            if(tag instanceof Building){
                String value = tag.getValue();
                if(value.equals("house") || value.equals("residential") || value.equals("apartments")){
                    return 0.5;
                }
            }
            else if(tag instanceof Landuse){
                if(tag.getValue().equals("residential")){
                    return 0.25;
                }
            }
            return 0;
        }
    }

}
