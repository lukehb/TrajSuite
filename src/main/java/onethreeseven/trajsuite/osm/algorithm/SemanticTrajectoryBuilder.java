package onethreeseven.trajsuite.osm.algorithm;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.util.GeometricShapeFactory;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.model.CompositePt;
import onethreeseven.datastructures.model.SpatioCompositeTrajectory;
import onethreeseven.geo.model.LatLonBounds;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.trajsuite.osm.model.PlaceExtractorSink;
import onethreeseven.trajsuite.osm.model.SemanticPlace;
import onethreeseven.trajsuite.osm.model.SemanticTrajectory;
import onethreeseven.trajsuite.osm.model.StopEpisode;
import onethreeseven.trajsuite.osm.model.tag.OsmTag;
import onethreeseven.trajsuite.osm.model.tag.OsmTagFilter;
import onethreeseven.trajsuite.osm.model.tag.Unknown;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.pbf2.v0_6.PbfReader;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;
import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

/**
 * Overall this class builds semantic trajectories from a series of stop-episodes.
 *
 * To achieve this there are a few inputs, namely:
 * <ul>
 *     <li>The stop-episodes trajectories (these are sequences of extended stops).
 *     <li>A geographic OSM extract for the relevant regions that these trajectories travelled.
 *     <li>A matching buffer (in metres).
 * </ul>
 * Once we have these inputs the semantic trajectories are built using a Hidden Markov Model (HMM).
 * A HMM allows us to "match" each {@link StopRegion} with the most likely {@link SemanticPlace} based on
 * the spatial proximity to the candidate places and also the overall potential place transitions possible
 * across the whole trajectory data-set.
 *
 * A HMM consists of three components,
 * 1) Starting probabilities (probability for each type of place),
 * 2) Emission Probabilities (probability of observing this particular place for this particular stop region), and
 * 3) Transition Probabilities (probability of observing this particular type of place given the previous type of place we think we observed).
 * For our problem we compute each of these as follows:
 * <ol>
 *    <li>Starting probabilities are computed as the normalised frequency of all the different place types that we have captured.</li>
 *    <li>Emission probabilities are computed by measuring the distance from the stop region's boundary to the boundary of the candidate places around it (intersecting/containment is a special case).</li>
 *    <li>The transition probabilities are computed as the normalised frequency of all the potential place type transitions between every contiguous pair of stop regions.</li>
 * </ol>
 * Given these three components we can probabilistically match stop regions with OSM places. There are, however, some assumptions to keep in mind
 * when using the output of this approach. The major one is that because transition probabilities are computed using the whole data-set they naturally
 * bias trips made between certain types of places. This means that if there is no similarity among places travelled by each entity in the data-set then
 * the transition probabilities will be fairly useless. Additionally, even if there is similarity among the place type transitions for some entities
 * in the data-set a trajectory with a completely unique path of place types will be weighted towards following the popular paths of the group.
 *
 * <p>
 * The other main problem is that stop regions may be matched with an unknown place. If many unknown places are found as
 * the candidates for stops this will obviously negatively bias the transition probabilities.
 * </p>
 *
 * <p>This being said, in our testing this approach seems effective if the entities are mostly visiting a certain set of place types.
 * For example, in our tests human mobility trajectories captured in a large city were matched quite well.</p>
 *
 * The actual matching occurs in {@link HMMPlaceMatching}.
 *
 * @author Luke Bermingham
 */
public class SemanticTrajectoryBuilder {

    ////////////////////////
    //Fields
    ////////////////////////

    private static final Logger log = Logger.getLogger(SemanticTrajectoryBuilder.class.getSimpleName());
    private static final OsmTag unknownTag = new Unknown("none");
    private static final GeometryFactory gf = new GeometryFactory();
    private static final GeometricShapeFactory sf = new GeometricShapeFactory(gf);

    private OsmTagFilter tagFilter = OsmTagFilter.ALL;
    private boolean useWebQueries = false;



    /**
     * Creates a consumer that eats OSM ways and their geometry and adds them to candidate stop regions.
     * @param regionStopTree An R-Tree containing regions that we accept ways in.
     * @param stopRegionBuffer Buffer in metres.
     * @return The way/geometry consumer.
     */
    private BiConsumer<Entity, Geometry> createWayProcessor(STRtree regionStopTree, int stopRegionBuffer){
        return (way, geometry) -> {
            List regions = regionStopTree.query(geometry.getEnvelopeInternal());
            if(regions.isEmpty()){
                return;
            }
            //turn the way into a geometric semantics
            SemanticPlace place = placeFromOSMEntity(way, tagFilter);

            //store the geometric places for the relevant stop regions
            for (Object region : regions) {
                StopRegion sr = (StopRegion) region;
                double distToCenter = geometry.distance(sr.centerPt);
                if(distToCenter <= (stopRegionBuffer + sr.stopEpisode.getStopRadiusMeters()) ){
                    double pr = SemanticTrajectoryBuilder.this.computeLogPlaceProbability(place, geometry, sr.getStopEpisode(), stopRegionBuffer);
                    CandidatePlace candidatePlace = new CandidatePlace(pr, place, geometry);
                    ((StopRegion) region).places.add(candidatePlace);
                }
            }
        };
    }

    ///////////////////////////////
    //STATIC methods
    ///////////////////////////////

    /**
     * Convert an OSM entity into a {@link SemanticPlace}.
     * @param osmEntity An OSM entity is something like a node or way.
     * @param tagFilter The tags to filter out.
     * @return A semantic place if the entity was resolved to a known and supported OSM tag, otherwise,
     * the entity is resolved to an {@link Unknown} tag with a name of "null".
     */
    public static SemanticPlace placeFromOSMEntity(Entity osmEntity, OsmTagFilter tagFilter){
        long wayId = osmEntity.getId();
        Collection<Tag> tags = osmEntity.getTags();

        OsmTag primaryTag = null;
        boolean nameEnglish = false;
        String name = null;

        for (Tag tag : tags) {
            if(tag.getKey().contains("name")){
                if(nameEnglish){
                    continue;
                }
                if(tag.getKey().contains(":en")){
                    nameEnglish = true;
                }
                name = tag.getValue();
            }
            else{
                OsmTag resolvedTag = tagFilter.resolve(tag.getKey(), tag.getValue());
                if(resolvedTag != null && primaryTag == null){
                    primaryTag = resolvedTag;
                }
                else if(resolvedTag != null && resolvedTag.getSpecificity().level > primaryTag.getSpecificity().level){
                    primaryTag = resolvedTag;
                }
            }
        }

        if(primaryTag == null){
            primaryTag = unknownTag;
        }
        if(name == null){
            name = "null";
        }

        return new SemanticPlace(String.valueOf(wayId), name, primaryTag);
    }

    public static void extractPlaces(File osmFile, PlaceExtractorSink sink){

        RunnableSource reader = null;



        if(FileUtil.getExtension(osmFile).equals("pbf")){
            reader = new PbfReader(osmFile, 4);

        }else if(FileUtil.getExtension(osmFile).equals("bz2")){
            reader = new XmlReader(osmFile, false, CompressionMethod.BZip2);
        }

        if(reader == null){
            log.severe("Reader was null, check that the OSM file is pbf or bz2.");
            return;
        }
        reader.setSink(sink);

        //extract places from osm
        log.info("Reading OSM file to extract places.");
        reader.run();
    }

    ///////////////////////////////
    //PUBLIC methods
    ///////////////////////////////

    /**
     * Convert spatio-temporal trajectories resolve lat/lon/time to semantics/time.
     * @param trajs The spatio-temporal trajectories.
     * @param osmFile The osm file to extract places resolve.
     * @param matchingBufferMeters How many meters to search around the stop regions for relevant places to match.
     * @return The traces converted to a collection of time and places.
     */
    public Map<String, SemanticTrajectory> run(Map<String, SpatioCompositeTrajectory<StopEpisode>> trajs,
                                               File osmFile,
                                               int matchingBufferMeters){
        HMMPlaceMatching matchingAlgo = new HMMPlaceMatching(matchingBufferMeters)
                .setQueryWebForBetterPlaces(useWebQueries);
        return run(trajs, osmFile, matchingBufferMeters, matchingAlgo);
    }

    /**
     * Convert spatio-temporal trajectories resolve lat/lon/time to semantics/time.
     * @param trajs The spatio-temporal trajectories.
     * @param osmFile The osm file to extract places resolve.
     * @param matchingBufferMeters How many meters to search around the stop regions for relevant places to match.
     * @param matchingAlgo The {@link IPlaceMatching} algorithm to use.
     * @return The traces converted to a collection of time and places.
     */
    public Map<String, SemanticTrajectory> run(Map<String, SpatioCompositeTrajectory<StopEpisode>> trajs,
                                               File osmFile, int matchingBufferMeters,
                                               IPlaceMatching matchingAlgo){
        //phase 1: associate places with stop regions
        AbstractGeographicProjection projection = trajs.values().iterator().next().getProjection();
        LatLonBounds studyRegion = makeStudyRegion(trajs, matchingBufferMeters);
        Map<String, ArrayList<StopRegion>> stopRegionTrajs = toStopRegionSequences(trajs);

        populateStopRegionTrajsWithStopPlaces(osmFile, stopRegionTrajs, projection, studyRegion, matchingBufferMeters);

        if(useWebQueries){
            //log.info("Enriching empty stop regions using FourSquare.");
            //enrichWithFourSquare(stopRegionTrajs, projection, matchingBufferMeters);
        }

        //phase: choose just one place to be matched with each stop region
        log.info("Starting place-matching...");
        return matchingAlgo.run(stopRegionTrajs, projection);
    }

    public SemanticTrajectoryBuilder setUseWebQueries(boolean useWebQueries) {
        this.useWebQueries = useWebQueries;
        return this;
    }

    /////////////////////
    //Private
    ////////////////////

    /**
     * This function associates potential stop places with their appropriate
     * stop regions. It does this like so:
     * First, an R-Tree is constructed that contains all of the stop regions with a
     * user-specified buffer around them. Next, an OSM file is processed and all
     * ways/nodes within the study region are checked to see which of the stop regions
     * intersect with them. All ways/nodes that intersecting with the buffered stop
     * regions are then associated with those regions as potential stop places. The
     * output of all this is the map of stop regions above has a collection of places
     * populated internally.
     * @param osmFile The OSM file to extract candidate stop places from.
     * @param stopRegionTrajs The sequences of stop regions (extracted from raw trajectories usually).
     * @param projection The geographic projection to use.
     * @param studyRegion The study region to extract places from.
     * @param matchingBufferMeters The number of meters to buffer around each stop region in search
     *                             of candidate places.
     */
    private void populateStopRegionTrajsWithStopPlaces(File osmFile,
                                                       Map<String, ArrayList<StopRegion>> stopRegionTrajs,
                                                       AbstractGeographicProjection projection,
                                                       LatLonBounds studyRegion,
                                                       int matchingBufferMeters){
        STRtree regionStopTree = createStopRegionTree(stopRegionTrajs, matchingBufferMeters);
        BiConsumer<Entity, Geometry> wayProcessor = createWayProcessor(regionStopTree, matchingBufferMeters);
        PlaceExtractorSink sink = new PlaceExtractorSink(projection, studyRegion, wayProcessor);
        extractPlaces(osmFile, sink);
    }

    private STRtree createStopRegionTree(Map<String, ArrayList<StopRegion>> stopRegions, int stopRegionBufferMeters){
        STRtree tree = new STRtree();
        //insert each stop region (with some buffer) into the R-tree
        for (ArrayList<StopRegion> trajectory : stopRegions.values()) {
            for (StopRegion stopRegion : trajectory) {
                double[] center = stopRegion.stopEpisode.getCoords();
                double hyp = stopRegion.stopEpisode.getStopRadiusMeters() + stopRegionBufferMeters;
                double xOffset = Math.cos(Math.PI*0.25) * hyp;
                double yOffset = Math.sin(Math.PI*0.25) * hyp;
                double minX = center[0] - xOffset;
                double minY = center[1] - yOffset;
                double maxX = center[0] + xOffset;
                double maxY = center[1] + yOffset;
                Envelope bounds = new Envelope(minX, maxX, minY, maxY);
                tree.insert(bounds, stopRegion);
            }
        }
        return tree;
    }

    private Map<String, ArrayList<StopRegion>> toStopRegionSequences(Map<String, SpatioCompositeTrajectory<StopEpisode>> trajs){
        HashMap<String, ArrayList<StopRegion>> out = new HashMap<>();
        for (Map.Entry<String, SpatioCompositeTrajectory<StopEpisode>> entry : trajs.entrySet()) {
            ArrayList<StopRegion> regions = new ArrayList<>();
            for (StopEpisode stopEpisode : entry.getValue()) {
                regions.add(new StopRegion(stopEpisode, new ArrayList<>()));
            }
            out.put(entry.getKey(), regions);
        }
        return out;
    }

//    private void enrichWithFourSquare(Map<String, ArrayList<StopRegion>> trajs, AbstractGeographicProjection proj, int bufferMeters){
//        FourSquarePlaceService service = new FourSquarePlaceService();
//        //find any stop regions that have no candidate places and try to enrich with foursquare
//        for (ArrayList<StopRegion> stopRegions : trajs.values()) {
//            for (StopRegion stopRegion : stopRegions) {
//                if(stopRegion.getPlaces().isEmpty()){
//
//                    double[] latlonStop = proj.cartesianToGeographic(stopRegion.getStopEpisode().getCoords());
//                    int searchRadius = (int) (stopRegion.getStopEpisode().getStopRadiusMeters() + bufferMeters);
//                    Collection<CompositePt<SemanticPlace>> nearbyPlaces = service.getNearbyPlaces(latlonStop[0], latlonStop[1], searchRadius);
//
//                    for (CompositePt<SemanticPlace> nearbyPlace : nearbyPlaces) {
//                        double[] latlonPlace = nearbyPlace.getCoords();
//                        double[] placeCoords = proj.geographicToCartesian(latlonPlace[0], latlonPlace[1]);
//                        Geometry placeCenter = gf.createPoint(new Coordinate(placeCoords[0], placeCoords[1]));
//                        double pr = computeLogPlaceProbability(nearbyPlace.getExtra(), placeCenter, stopRegion.getStopEpisode(), bufferMeters);
//                        CandidatePlace geomPlace = new CandidatePlace(pr, nearbyPlace.getExtra(), placeCenter);
//                        stopRegion.getPlaces().add(geomPlace);
//                    }
//                }
//            }
//        }
//    }

    private LatLonBounds makeStudyRegion(Map<String, SpatioCompositeTrajectory<StopEpisode>> trajs, int stopRegionBufferMeters){
        AbstractGeographicProjection projection = trajs.values().iterator().next().getProjection();

        //here we are assuming a flat earth projection
        //get min/max x/y/radius
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxRadius = Double.NEGATIVE_INFINITY;

        for (SpatioCompositeTrajectory<StopEpisode> traj : trajs.values()) {
            if(!traj.isInCartesianMode()){
                traj.toCartesian();
            }

            for (StopEpisode stopEpisode : traj) {
                double[] coord = stopEpisode.getCoords();
                double radius = stopEpisode.getStopRadiusMeters();
                double x = coord[0];
                double y = coord[1];
                if(x < minX){
                    minX = x;
                }
                if(y < minY){
                    minY = y;
                }
                if(x > maxX){
                    maxX = x;
                }
                if(y > maxY){
                    maxY = y;
                }
                if(radius > maxRadius){
                    maxRadius = radius;
                }
            }
        }

        //we have min and max, now we can extend it
        double offset = maxRadius + stopRegionBufferMeters;
        double[] botLeft = new double[]{minX-offset, minY-offset};
        double[] topRight = new double[]{maxX+offset, maxY+offset};
        double[] botLeftGeo = projection.cartesianToGeographic(botLeft);
        double[] topRightGeo = projection.cartesianToGeographic(topRight);
        return new LatLonBounds(botLeftGeo[0], topRightGeo[0], botLeftGeo[1], topRightGeo[1]);
    }

    private double computeLogPlaceProbability(SemanticPlace semantics, Geometry potentialPlace, StopEpisode observedStop, double maxSearchRadius){
        double[] centerCoords = observedStop.getCoords();
        Coordinate center = new Coordinate(centerCoords[0], centerCoords[1]);
        sf.setCentre(center);
        //for whatever reason it expects size of circle as diameter
        sf.setSize(observedStop.getStopRadiusMeters() * 2);
        sf.setNumPoints(8);

        //Geometry stopCenter = gf.createPoint(center);
        Geometry stopGeom = sf.createCircle();


        //case: the stop is at least somewhat within the place
        if(stopGeom.intersects(potentialPlace)){

            if(stopGeom.contains(potentialPlace)){
                return 0;
            }

            double placeArea = potentialPlace.getArea();
            double intersectingArea = stopGeom.intersection(potentialPlace).getArea();
            double pr = 0.5 + (intersectingArea/placeArea * 0.5);
            return Math.log(pr);
        }
        //case: the place is not within the stop center so give a probability based on
        //how close the place is to the stop radius
        //note that, even if it is touching the stop radius the max stop pr is 50%
        else{

            double distanceToCenter = potentialPlace.distance(stopGeom);

            if(distanceToCenter > (observedStop.getStopRadiusMeters() + maxSearchRadius)){
                return Math.log(1e-7);
            }

            double pr;

            if(distanceToCenter > observedStop.getStopRadiusMeters()){
                pr = (maxSearchRadius - (distanceToCenter - observedStop.getStopRadiusMeters()))/maxSearchRadius;
            }
            else{
                pr = (maxSearchRadius - distanceToCenter) / maxSearchRadius;
            }

            pr *= 0.5;
            return Math.log(pr);
        }
    }

    ////////////////////////
    //Private inner classes
    ////////////////////////

    /**
     * A stop region is a geographic region (expressed as a center and radius)
     * and can have a number of OSM ways inside or intersecting with it.
     * Our task is then to find which of these OSM ways is mostly the place
     * that the stop was occurring at.
     */
    class StopRegion{
        private final StopEpisode stopEpisode;
        private final ArrayList<CandidatePlace> places;
        private final Point centerPt;

        StopRegion(StopEpisode stopEpisode, ArrayList<CandidatePlace> places) {
            this.stopEpisode = stopEpisode;
            this.places = places;
            this.centerPt = gf.createPoint(new Coordinate(stopEpisode.getCoords()[0], stopEpisode.getCoords()[1]));
        }

        StopEpisode getStopEpisode() {
            return stopEpisode;
        }

        ArrayList<CandidatePlace> getPlaces() {
            return places;
        }
    }

    /**
     * A container class holding actual OSM geometry for the place
     * and also the semantic details of the place (i.e the OSM key/value tag).
     */
    class CandidatePlace {
        private final Geometry geometry;
        private final double probability;
        private final SemanticPlace semantics;

        CandidatePlace(double probability, SemanticPlace semantics, Geometry geometry) {
            this.probability = probability;
            this.semantics = semantics;
            this.geometry = geometry;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        double getProbability() {
            return probability;
        }

        SemanticPlace getSemantics() {
            return semantics;
        }

    }

}
