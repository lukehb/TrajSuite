package onethreeseven.trajsuite.osm.algorithm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.util.GeometricShapeFactory;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.trajsuite.osm.model.SemanticPlace;
import onethreeseven.trajsuite.osm.model.SemanticTrajectory;
import onethreeseven.trajsuite.osm.model.TimeAndPlace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple place-matching that matches each stop region with the first nearest place.
 * @author Luke Bermingham
 */
public class NaivePlaceMatching implements IPlaceMatching{


    private static final GeometryFactory gf = new GeometryFactory();
    private static final GeometricShapeFactory sf = new GeometricShapeFactory(gf);

    @Override
    public Map<String, SemanticTrajectory> run(Map<String, ArrayList<SemanticTrajectoryBuilder.StopRegion>> trajs,
                                               AbstractGeographicProjection projection){

        //go through and assign each nearest place as the likely stop
        Map<String, SemanticTrajectory> output = new HashMap<>();
        for (Map.Entry<String, ArrayList<SemanticTrajectoryBuilder.StopRegion>> entry : trajs.entrySet()) {

            SemanticTrajectory semanticTraj = new SemanticTrajectory(true, projection);
            for (SemanticTrajectoryBuilder.StopRegion stopRegion : entry.getValue()) {
                SemanticPlace nearestPlace = extractNearestPlace(stopRegion);
                if(nearestPlace != null){
                    TimeAndPlace timeAndPlace = new TimeAndPlace(stopRegion.getStopEpisode().getTime(),
                            stopRegion.getStopEpisode().getTimeCategory(), nearestPlace);
                    semanticTraj.addCartesian(stopRegion.getStopEpisode().getCoords(), timeAndPlace);
                }
            }
            output.put(entry.getKey(), semanticTraj);
        }
        return output;
    }

    private SemanticPlace extractNearestPlace(SemanticTrajectoryBuilder.StopRegion stopRegion){

        SemanticTrajectoryBuilder.CandidatePlace nearestPlace = null;
        double bestDist = Double.MAX_VALUE;

        final double[] centerCoords = stopRegion.getStopEpisode().getCoords();
        final Coordinate center = new Coordinate(centerCoords[0], centerCoords[1]);
        sf.setCentre(center);
        sf.setSize(stopRegion.getStopEpisode().getStopRadiusMeters() * 2);
        sf.setNumPoints(8);
        final Geometry stopGeom = sf.createCircle();

        for (SemanticTrajectoryBuilder.CandidatePlace candidatePlace : stopRegion.getPlaces()) {
            double distToPlace = stopGeom.distance(candidatePlace.getGeometry());
            if(distToPlace < bestDist){
                nearestPlace = candidatePlace;
                bestDist = distToPlace;
            }
        }

        return nearestPlace == null ? null : nearestPlace.getSemantics();
    }

}
