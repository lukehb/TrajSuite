package onethreeseven.trajsuite.osm.algorithm;

import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.trajsuite.osm.model.SemanticTrajectory;
import java.util.ArrayList;
import java.util.Map;

/**
 * Interface for place-matching algorithms.
 * @author Luke Bermingham
 */
public interface IPlaceMatching {

    /**
     * Matches stop regions to their likely place candidate.
     * @param trajs A map of stop-regions each with associated place candidates.
     * @param projection The geographic projection we are using.
     * @return A map of semantic trajectories.
     */
    Map<String, SemanticTrajectory> run(Map<String, ArrayList<SemanticTrajectoryBuilder.StopRegion>> trajs, AbstractGeographicProjection projection);

}
