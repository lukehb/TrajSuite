package onethreeseven.trajsuite.osm.model;

import onethreeseven.datastructures.model.SpatioCompositeTrajectory;
import onethreeseven.geo.projection.AbstractGeographicProjection;

/**
 * A trajectory made of spatial coordinates, semantic
 * times (i.e afternoon) and places (Foobar's burgers).
 * @author Luke Bermingham
 */
public class SemanticTrajectory extends SpatioCompositeTrajectory<SemanticPt> {

    public SemanticTrajectory(){
        super();
    }

    public SemanticTrajectory(boolean inCartesianMode, AbstractGeographicProjection projection){
        super(inCartesianMode, projection);
    }

    public void addCartesian(double[] coords, TimeAndPlace timeAndPlace) {
        this.addCartesian(new SemanticPt(coords, timeAndPlace));
    }

    public void addGeographic(double[] coords, TimeAndPlace timeAndPlace) {
        this.addGeographic(new SemanticPt(coords, timeAndPlace));
    }

    /**
     * Calculates a similarity (f-measure) between this trajectory and some other
     * by comparing the places at each entry.
     * @param otherTraj The other trajectory.
     * @return 0 if no similar places at any entry, 1 if entry has the same place.
     */
    public double calculateSimilarity(SemanticTrajectory otherTraj){

        if(otherTraj == null){
            return 0;
        }

        double tp = 0;
        double fp = 0;
        double fn = 0;

        for (int j = 0; j < this.size(); j++) {
            SemanticPlace truePlace = this.get(j).getTimeAndPlace().getPlace();
            if(j > otherTraj.size()-1){
                fn = this.size() - otherTraj.size();
                break;
            }
            SemanticPlace estimatedPlace = otherTraj.get(j).getTimeAndPlace().getPlace();
            if(truePlace.equals(estimatedPlace)){
                tp++;
            }else{
                fp++;
            }
        }

        double precision = (tp+fp == 0) ?
                0 :
                tp/(tp+fp);

        double truePositiveRate = (tp+fn == 0) ?
                0 :
                tp/(tp+fn);

        //f-score
        return (precision + truePositiveRate == 0) ?
                0 :
                2 * (precision * truePositiveRate)/(precision + truePositiveRate);

    }


}
