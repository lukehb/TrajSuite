package onethreeseven.trajsuite.osm.data;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.kdtree.KdNode;
import com.vividsolutions.jts.index.kdtree.KdTree;
import onethreeseven.common.model.TimeCategoryPool;
import onethreeseven.datastructures.data.SpatioCompositeTrajectoryWriter;
import onethreeseven.datastructures.model.CompositePt;
import onethreeseven.geo.model.LatLonBounds;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.trajsuite.osm.algorithm.SemanticTrajectoryBuilder;
import onethreeseven.trajsuite.osm.model.*;
import onethreeseven.trajsuite.osm.model.tag.OsmTag;
import onethreeseven.trajsuite.osm.model.tag.OsmTagFilter;
import onethreeseven.trajsuite.osm.model.tag.Unknown;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Generates a synthetic semantic trajectory.
 * @author Luke Bermingham
 */
public class SyntheticSemanticTrajectoryGenerator {

    /**
     * Make synthetic semantic trajectories.
     * @param projection The geographic projection.
     * @param osmFile The OSM file to find real places from.
     * @param nPlaces The number of total places allowed in a our pool of places.
     * @param nTrajs The number of trajectories to make.
     * @param nEpisodes The number of stop and move episodes the trajectory should make.
     * @param nEntriesPerEpisode The number of entries in each trajectory stop or move episode.
     * @param spatialNoise The amount of spatial noise/jitter (in meters) at each stop.
     * @param startTime The start time of that the trajectories are tracked from.
     * @param episodeDuration The duration of the stop (in milliseconds).
     * @param durationMultiplier A multiplier applied to the stop duration (multiplier * random) * stopDuration.
     * @param timeCategoryPool The time categories to classify the temporal dimension into.
     * @param processor The thing that processes the {@link SemanticTrajectory}s produced by this function.
     */
    public void makeTrajectories(AbstractGeographicProjection projection,
                                 File osmFile,
                                 int nPlaces,
                                 int nTrajs,
                                 int nEpisodes,
                                 int nEntriesPerEpisode,
                                 double spatialNoise,
                                 LocalDateTime startTime,
                                 long episodeDuration,
                                 double durationMultiplier,
                                 TimeCategoryPool timeCategoryPool,
                                 Consumer<SemanticTrajectory> processor){

        Table<String, Double> transitionMatrix;
        Map<String, List<CompositePt<SemanticPlace>>> placeMap;
        {
            List<CompositePt<SemanticPlace>> placeRepo = extractPlaces(osmFile, projection, OsmTagFilter.ALL, spatialNoise);
            placeRepo = reducePlaces(placeRepo, nPlaces);
            transitionMatrix = generateTransitionMatrix(placeRepo);
            placeMap = new HashMap<>();
            for (CompositePt<SemanticPlace> placePt : placeRepo) {
                String placeType = placePt.getExtra().getPrimaryTag().getName();
                List<CompositePt<SemanticPlace>> storedPlaces = placeMap.get(placeType);
                if(storedPlaces == null){
                    storedPlaces = new ArrayList<>();
                    placeMap.put(placeType, storedPlaces);
                }
                storedPlaces.add(placePt);
            }
        }

        for (int i = 0; i < nTrajs; i++) {
            SemanticTrajectory traj = generate(projection,
                    nEpisodes,
                    nEntriesPerEpisode,
                    spatialNoise,
                    startTime,
                    episodeDuration,
                    durationMultiplier,
                    timeCategoryPool,
                    placeMap,
                    transitionMatrix);
            processor.accept(traj);
        }
    }

    /**
     * Make synthetic semantic trajectories and write them one by one to a file.
     * @param projection The geographic projection.
     * @param osmFile The OSM file to find real places from.
     * @param nPlaces The number of total places allowed in a our pool of places.
     * @param nTrajs The number of trajectories to make.
     * @param nEpisodes The number of stop and move episodes the trajectory should make.
     * @param nEntriesPerEpisode The number of entries in each trajectory stop or move episode.
     * @param spatialNoise The amount of spatial noise/jitter (in meters) at each stop.
     * @param startTime The start time of that the trajectories are tracked from.
     * @param episodeDuration The duration of the stop (in milliseconds).
     * @param durationMultiplier A multiplier applied to the stop duration (multiplier * random) * stopDuration.
     * @param timeCategoryPool The time categories to classify the temporal dimension into.
     * @param outputFile The file where the semantic trajectories are written.
     */
    public void makeTrajectories(AbstractGeographicProjection projection,
                                                            File osmFile,
                                                            int nPlaces,
                                                            int nTrajs,
                                                            int nEpisodes,
                                                            int nEntriesPerEpisode,
                                                            double spatialNoise,
                                                            LocalDateTime startTime,
                                                            long episodeDuration,
                                                            double durationMultiplier,
                                                            TimeCategoryPool timeCategoryPool,
                                                            File outputFile){

        final SpatioCompositeTrajectoryWriter writer = new SpatioCompositeTrajectoryWriter();
        final AtomicInteger id = new AtomicInteger();
        final Consumer<SemanticTrajectory> trajProcessor = trajectory -> {
            String key = String.valueOf(id.getAndIncrement());
            writer.write(outputFile, Collections.singletonMap(key, trajectory));
        };

        makeTrajectories(projection, osmFile, nPlaces, nTrajs, nEpisodes, nEntriesPerEpisode, spatialNoise, startTime,
                episodeDuration, durationMultiplier, timeCategoryPool, trajProcessor);
    }

    /**
     * Make synthetic semantic trajectories and store them in a map.
     * @param projection The geographic projection.
     * @param osmFile The OSM file to find real places from.
     * @param nPlaces The number of total places allowed in a our pool of places.
     * @param nTrajs The number of trajectories to make.
     * @param nEpisodes The number of stop and move episodes the trajectory should make.
     * @param nEntriesPerEpisode The number of entries in each trajectory stop or move episode.
     * @param spatialNoise The amount of spatial noise/jitter (in meters) at each stop.
     * @param startTime The start time of that the trajectories are tracked from.
     * @param episodeDuration The duration of the stop (in milliseconds).
     * @param durationMultiplier A multiplier applied to the stop duration (multiplier * random) * stopDuration.
     * @param timeCategoryPool The time categories to classify the temporal dimension into.
     * @return A map of key-value {@link SemanticTrajectory}s.
     */
    public Map<String, SemanticTrajectory> makeTrajectories(AbstractGeographicProjection projection,
                                                            File osmFile,
                                                            int nPlaces,
                                                            int nTrajs,
                                                            int nEpisodes,
                                                            int nEntriesPerEpisode,
                                                            double spatialNoise,
                                                            LocalDateTime startTime,
                                                            long episodeDuration,
                                                            double durationMultiplier,
                                                            TimeCategoryPool timeCategoryPool){

        final Map<String, SemanticTrajectory> out = new HashMap<>();
        final AtomicInteger id = new AtomicInteger();
        final Consumer<SemanticTrajectory> trajProcessor = trajectory -> {
            String key = String.valueOf(id.getAndIncrement());
            out.put(key, trajectory);
        };
        makeTrajectories(projection, osmFile, nPlaces, nTrajs, nEpisodes, nEntriesPerEpisode, spatialNoise, startTime,
                episodeDuration, durationMultiplier, timeCategoryPool, trajProcessor);
        return out;
    }


    /**
     * Using an OSM file, a tag filter, and a study region extract all relevant OSM ways/node places.
     * @param osmFile The OSM to extract places from.
     * @param projection The projection to store the places in.
     * @param tagFilter The supported OSM tags we are interested in.
     * @return A map of all discovered places where the key/value mapping is PlaceType/Places.
     */
    public List<CompositePt<SemanticPlace>> extractPlaces(File osmFile,
                                                          AbstractGeographicProjection projection,
                                                          OsmTagFilter tagFilter,
                                                          double noise){

        final List<CompositePt<SemanticPlace>> placeRepository = new ArrayList<>();

        final Random r = new Random();

        BiConsumer<Entity, Geometry> placeConsumer = (entity, geometry) -> {
            SemanticPlace place = SemanticTrajectoryBuilder.placeFromOSMEntity(entity, tagFilter);
            if(place.getSpecificity() > OsmTag.Specificity.GENERIC.level && !place.getPlaceName().equals("null")){
                Coordinate vert = geometry.getCoordinate();
                double[] coord = new double[]{
                        vert.x + (r.nextDouble() * noise),
                        vert.y + (r.nextDouble() * noise)
                };
                SpatioSemanticPt pt = new SpatioSemanticPt(coord, place);
                placeRepository.add(pt);
            }
        };

        PlaceExtractorSink sink = new PlaceExtractorSink(projection, LatLonBounds.FULL_SPHERE, placeConsumer);
        SemanticTrajectoryBuilder.extractPlaces(osmFile, sink);

        return placeRepository;
    }

    public static void compressSemanticTrajectory(SemanticTrajectory trajectory){

        SemanticPt prev = null;
        Iterator<SemanticPt> entryIter = trajectory.iterator();
        while(entryIter.hasNext()){
            SemanticPt cur = entryIter.next();
            if(prev != null){
                if(prev.getTimeAndPlace().getPlace().equals(cur.getTimeAndPlace().getPlace())
                        || cur.getTimeAndPlace().getPlace().getPrimaryTag() instanceof Unknown){
                    entryIter.remove();
                }
            }
            prev = cur;
        }

    }



    private List<CompositePt<SemanticPlace>> reducePlaces(List<CompositePt<SemanticPlace>> places,
                                                                            int nPlaces){
        Collections.shuffle(places);

        KdTree tree = new KdTree();


        for (CompositePt<SemanticPlace> place : places) {
            double[] coords = place.getCoords();
            double x = coords[0];
            double y = coords[1];
            tree.insert(new Coordinate(x,y), place);
        }

        final ArrayList<CompositePt<SemanticPlace>> ambiguousPlaces = new ArrayList<>();
        final double searchBuffer = 10;

        for (CompositePt<SemanticPlace> place : places) {

            double centerX = place.getCoords()[0];
            double centerY = place.getCoords()[1];
            double lowerX = centerX - searchBuffer*0.5;
            double upperX = centerX + searchBuffer*0.5;
            double lowerY = centerY - searchBuffer*0.5;
            double upperY = centerY + searchBuffer*0.5;

            List qrPlaces = tree.query(new Envelope(lowerX, upperX, lowerY, upperY));
            if(qrPlaces.size() > 2){
                for (Object node : qrPlaces) {
                    KdNode kdNode = (KdNode) node;
                    CompositePt<SemanticPlace> actaulPt = (CompositePt<SemanticPlace>) kdNode.getData();
                    ambiguousPlaces.add(actaulPt);
                }
            }

            if(ambiguousPlaces.size() >= nPlaces){
                System.out.println("Found " + nPlaces + " that are within " + searchBuffer + "m of each other.");
                return ambiguousPlaces;
            }

        }

        System.out.println("Could not find close places, just picking random ones.");
        return places.subList(0, Math.min(nPlaces-1, places.size()-1));
    }

    private Table<String, Double> generateTransitionMatrix(Collection<CompositePt<SemanticPlace>> placeRepository){

        //find all the types of places we have
        Set<String> placeTypes = new HashSet<>();
        for (CompositePt<SemanticPlace> place : placeRepository) {
            placeTypes.add(place.getExtra().getPrimaryTag().getName());
        }

        final Random r = new Random();

        Table<String, Double> transitionMatrix = new Table<>();

        //generate prs for all place to place transitions
        for (String placeType : placeTypes) {
            double probabilityPool = 1.0;
            for (String otherPlaceType : placeTypes) {
                double pr = r.nextDouble() * probabilityPool;
                probabilityPool -= pr;
                double logLikelihood = Math.log(pr);
                transitionMatrix.put(placeType, otherPlaceType, logLikelihood);
            }
        }

        return transitionMatrix;

    }

    private SemanticTrajectory generate(AbstractGeographicProjection projection,
                                       int nEpisodes,
                                       int nEntriesPerEpisode,
                                       double spatialNoise,
                                       LocalDateTime startTime,
                                       long episodeDurationMillis,
                                       double durationMultiplier,
                                       TimeCategoryPool timeCategoryPool,
                                       Map<String, List<CompositePt<SemanticPlace>>> placeMap,
                                       Table<String, Double> transitionMatrix){
        //make a trajectory that is uses the transition matrix to decide place visitations
        SemanticTrajectory traj = new SemanticTrajectory(true, projection);

        final List<String> placeTypes = new ArrayList<>(placeMap.keySet());

        CompositePt<SemanticPlace> prevPt = null;
        LocalDateTime curTime = startTime;

        final SemanticPlace unknown = new SemanticPlace("-1", "null", new Unknown("null"));

        for (int i = 0; i < nEpisodes; i++) {
            prevPt = pickNextStopPlace(prevPt, placeMap, placeTypes, transitionMatrix);
            if(prevPt != null){
                //do stop
                curTime = doEpisode(traj, prevPt.getCoords(), curTime, prevPt.getExtra(),
                        episodeDurationMillis, durationMultiplier, timeCategoryPool, nEntriesPerEpisode, spatialNoise, false);
                //do move
                curTime = doEpisode(traj, prevPt.getCoords(), curTime, unknown,
                        episodeDurationMillis, durationMultiplier, timeCategoryPool, nEntriesPerEpisode, spatialNoise, true);
            }
        }
        return traj;
    }

    private CompositePt<SemanticPlace> pickNextStopPlace(CompositePt<SemanticPlace> prevStop,
                                                         Map<String, List<CompositePt<SemanticPlace>>> placeMap,
                                                         List<String> placeTypes,
                                                         Table<String, Double> transitionMatrix){
        Random r = new Random();

        //previous stop not known, so just randomly pick one
        if(prevStop == null){
            int placeTypeIdx = r.nextInt(placeTypes.size());
            String placeType = placeTypes.get(placeTypeIdx);
            List<CompositePt<SemanticPlace>> potentialPlaces = placeMap.get(placeType);
            int placeIdx = r.nextInt(potentialPlaces.size());
            return potentialPlaces.get(placeIdx);
        }
        //the previous stop is known, so use transition matrix to pick the type of place
        else{
            Map<String, Double> placeTypePrs = transitionMatrix.getRow(prevStop.getExtra().getPrimaryTag().getName());

            double bestPr = Double.NEGATIVE_INFINITY;
            String mostLikelyPlaceType = null;

            for (Map.Entry<String, Double> entry : placeTypePrs.entrySet()) {
                double logliklihood = Math.log(r.nextDouble()) + entry.getValue();
                if(logliklihood > bestPr){
                    bestPr = logliklihood;
                    mostLikelyPlaceType = entry.getKey();
                }
            }

            if(mostLikelyPlaceType != null){
                List<CompositePt<SemanticPlace>> potentialPlaces = placeMap.get(mostLikelyPlaceType);
                int placeIdx = r.nextInt(potentialPlaces.size());
                return potentialPlaces.get(placeIdx);
            }

        }
        return null;
    }

    private LocalDateTime doEpisode(SemanticTrajectory traj,
                                    final double[] startCoordinate,
                                    LocalDateTime episodeBeginTime,
                                    SemanticPlace episodePlace,
                                    long episodeDurationMillis,
                                    double durationMultiplier,
                                    TimeCategoryPool timeCategoryPool,
                                    int nEntriesPerEpisode,
                                    double spatialNoise,
                                    boolean isMove){

        final Random r = new Random();
        final long episodeDuration = (long) (episodeDurationMillis * (1 + (r.nextDouble() * durationMultiplier)));
        final long deltaMillis = episodeDuration/nEntriesPerEpisode;

        LocalDateTime curTime = episodeBeginTime;
        double[] prevCoord = startCoordinate;

        for (int i = 0; i < nEntriesPerEpisode; i++) {
            curTime = episodeBeginTime.plus(deltaMillis * i, ChronoUnit.MILLIS);

            double[] coords;

            //case: emulate a stopping coordinate
            if(!isMove){
                double noiseX = ((r.nextDouble() * 2) - 1) * spatialNoise;
                double noiseY = ((r.nextDouble() * 2) - 1) * spatialNoise;
                coords = new double[]{
                        startCoordinate[0] + noiseX,
                        startCoordinate[1] + noiseY
                };

            }
            //case: emulate a extremely noisy coordinate (moving)
            else{
                coords = new double[]{
                        prevCoord[0] + (1 + r.nextDouble()) * spatialNoise * 1000,
                        prevCoord[1] + (1 + r.nextDouble()) * spatialNoise * 1000,
                };
                prevCoord = new double[]{coords[0], coords[1]};
            }

            TimeAndPlace timeAndPlace = new TimeAndPlace(curTime, timeCategoryPool.resolve(curTime), episodePlace);
            traj.addCartesian(coords, timeAndPlace);
        }
        return curTime;
    }

    /////////////////////////////////
    //PRIVATE CLASSES
    /////////////////////////////////

    private class SpatioSemanticPt extends CompositePt<SemanticPlace>{

        private final SemanticPlace place;

        protected SpatioSemanticPt(double[] coords, SemanticPlace semanticPlace) {
            super(coords);
            this.place = semanticPlace;
        }

        @Override
        public SemanticPlace getExtra() {
            return place;
        }

        @Override
        public String printExtra(String s) {
            return place.print(s);
        }
    }


}
