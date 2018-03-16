package onethreeseven.trajsuite.osm.algorithm;

import onethreeseven.common.model.TimeCategory;
import onethreeseven.common.model.TimeCategoryPool;
import onethreeseven.common.util.Maths;
import onethreeseven.common.util.NDUtil;
import onethreeseven.datastructures.model.STStopPt;
import onethreeseven.datastructures.model.STStopTrajectory;
import onethreeseven.datastructures.model.SpatioCompositeTrajectory;
import onethreeseven.trajsuite.osm.model.StopEpisode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

/**
 * Converts some raw spatio-temporal traces into stop episodes.
 * @author Luke Bermingham
 */
public class SeClust {

    /**
     * Converts a trajectory within individually annotated stops and moves into a trajectory
     * of stop episodes.
     * @param traj The stop/move annotated trajectory to convert.
     * @param minMoveDurationMillis If move episode is less than this it becomes a stop also.
     * @param minEpisodeMillis The minimum amount of time for a stop episode to be considered an episode.
     * @param timeCategoryPool The time category pool used to discretise the time dimension into a category.
     * @return A trajectory made of distinct stop episodes.
     */
    public SpatioCompositeTrajectory<StopEpisode> run(STStopTrajectory traj,
                                                      long minMoveDurationMillis,
                                                      long minEpisodeMillis,
                                                      TimeCategoryPool timeCategoryPool){
        cleanupStops(traj, minMoveDurationMillis);
        return toStopEpisodes(traj, minEpisodeMillis, timeCategoryPool);
    }

    /**
     * Merges contiguous stops with a small enough temporal gap into one bigger set of stops.
     * @param traj The stop traj.
     * @param nearbyStopMillis How small the move gap has to be.
     */
    private void cleanupStops(STStopTrajectory traj, long nearbyStopMillis){

        //algo works by going from one stop end to another stop beginning

        for (int i = 0; i < traj.size(); i++) {
            STStopPt entry = traj.get(i);
            boolean inAStop = entry.isStopped();
            //found the start of a stop
            if(inAStop){
                //traverse through the stop
                int moveStartIdx = i;
                for (; i < traj.size() && inAStop; i++) {
                    entry = traj.get(i);
                    moveStartIdx = i;
                    inAStop = entry.isStopped();
                }
                //now we are in a move, traverse through it
                int moveEndIdx = moveStartIdx;
                for (; i < traj.size() && !inAStop; i++) {
                    entry = traj.get(i);
                    moveEndIdx = i;
                    inAStop = entry.isStopped();
                }
                //we have ended up in a new stop
                if(inAStop){
                    long moveDurationMillis = ChronoUnit.MILLIS.between(
                            traj.getTime(moveStartIdx),
                            traj.getTime(moveEndIdx));
                    //make the in-between moves all into stops because they are between two nearby stops
                    if(moveDurationMillis <= nearbyStopMillis){
                        for (int j = moveStartIdx; j < moveEndIdx; j++) {
                            traj.get(j).setIsStopped(true);
                        }
                    }
                }
            }
        }


    }

    private SpatioCompositeTrajectory<StopEpisode> toStopEpisodes(STStopTrajectory traj,
                                                                  long minEpisodeMillis,
                                                                  TimeCategoryPool timeCategoryPool){

        ArrayList<double[]> coords = new ArrayList<>();
        LocalDateTime startEpisode = null;
        LocalDateTime endEpisode = null;
        boolean isInStop = false;

        //make sure we are in cartesian coordinates
        if(!traj.isInCartesianMode()){
            traj.toCartesian();
        }

        SpatioCompositeTrajectory<StopEpisode> output =
                new SpatioCompositeTrajectory<StopEpisode>(true, traj.getProjection()) {};

        for (int i = 0; i < traj.size(); i++) {
            STStopPt entry = traj.get(i);
            boolean curStopped = entry.isStopped();
            //case: this is the first stop
            if(!isInStop && curStopped){
                isInStop = true;
                coords.add(entry.getCoords());
                startEpisode = entry.getExtra();
                endEpisode = entry.getExtra();
            }
            //case: this is not the first stop
            else if(isInStop && curStopped){
                coords.add(entry.getCoords());
                endEpisode = entry.getExtra();
            }
            //case: in a stop, but we processed a move (or ran out of entries), so make a stop episode
            if(isInStop && !curStopped || isInStop && i == traj.size() - 1){
                isInStop = false;

                long durationMillis = ChronoUnit.MILLIS.between(startEpisode, endEpisode);
                //only permit stop episode of minimum duration or larger
                if(durationMillis >= minEpisodeMillis){
                    output.add(makeStopEpisode(coords, startEpisode, timeCategoryPool));
                }

                coords.clear();
                startEpisode = null;
                endEpisode = null;
            }
        }
        return output;
    }

    private StopEpisode makeStopEpisode(ArrayList<double[]> coords, LocalDateTime start, TimeCategoryPool timeCategoryPool){
        double[][] pts = new double[coords.size()][coords.iterator().next().length];
        for (int i = 0; i < coords.size(); i++) {
            pts[i] = coords.get(i);
        }
        double[] centerPt = NDUtil.averagePt(pts);
        //measure distance from center point to find our radius
        double maxRadius = 0;
        for (double[] otherPt : pts) {
            double curRadius = Maths.dist(centerPt, otherPt);
            if(curRadius > maxRadius){
                maxRadius = curRadius;
            }
        }
        TimeCategory timeCategory = timeCategoryPool.resolve(start);

        return new StopEpisode(centerPt, maxRadius, start, timeCategory);
    }

}
