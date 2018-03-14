package onethreeseven.trajsuite.osm.data;

import onethreeseven.common.model.TimeCategory;
import onethreeseven.common.model.TimeCategoryPool;
import onethreeseven.datastructures.data.AbstractTrajectoryParser;
import onethreeseven.datastructures.data.resolver.IdResolver;
import onethreeseven.datastructures.data.resolver.NumericFieldsResolver;
import onethreeseven.datastructures.data.resolver.TemporalFieldResolver;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.geo.projection.ProjectionMercator;
import onethreeseven.trajsuite.osm.model.SemanticPlace;
import onethreeseven.trajsuite.osm.model.SemanticTrajectory;
import onethreeseven.trajsuite.osm.model.TimeAndPlace;
import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Read in some data and tries to parse it into
 * a map of {@link SemanticTrajectory}.
 * @author Luke Bermingham
 */
public class SemanticTrajectoryParser extends AbstractTrajectoryParser<SemanticTrajectory> {

    private AbstractGeographicProjection projection = new ProjectionMercator();
    private boolean inCartesianMode = false;
    private TemporalFieldResolver temporalFieldResolver;
    private TimeCategoryPool timeCategoryPool;
    private SemanticPlaceFieldsResolver placeResolver;

    public SemanticTrajectoryParser(AbstractGeographicProjection projection,
                                    IdResolver idResolver,
                                    NumericFieldsResolver numericFieldsResolver,
                                    TemporalFieldResolver temporalFieldResolver,
                                    SemanticPlaceFieldsResolver placeResolver,
                                    TimeCategoryPool timeCategoryPool,
                                    boolean inCartesianMode) {
        super(idResolver, numericFieldsResolver);
        this.projection = projection;
        this.temporalFieldResolver = temporalFieldResolver;
        this.placeResolver = placeResolver;
        this.inCartesianMode = inCartesianMode;
        this.timeCategoryPool = timeCategoryPool;
    }

    @Override
    protected SemanticTrajectory makeNewTrajectory() {
        return new SemanticTrajectory(inCartesianMode, projection);
    }

    @Override
    protected void addCoordinates(SemanticTrajectory traj, double[] coords, String[] lineParts) {
        LocalDateTime t = temporalFieldResolver.resolve(lineParts);
        TimeCategory timeCategory = timeCategoryPool.resolve(t);
        SemanticPlace place = placeResolver.resolve(lineParts);
        TimeAndPlace timeAndPlace = new TimeAndPlace(t, timeCategory, place);

        if(inCartesianMode){
            traj.addCartesian(coords, timeAndPlace);
        }else{
            traj.addGeographic(coords, timeAndPlace);
        }
    }

    @Override
    protected String getCommandStringParams() {
        throw new UnsupportedOperationException("Load semantic trajectories not yet supported");
    }

    @Override
    public SemanticTrajectoryParser setNumericFieldsResolver(NumericFieldsResolver numericFieldsResolver) {
        super.setNumericFieldsResolver(numericFieldsResolver);
        return this;
    }

    @Override
    public AbstractTrajectoryParser<SemanticTrajectory> setIdResolver(IdResolver idResolver) {
        super.setIdResolver(idResolver);
        return this;
    }

    @Override
    public AbstractTrajectoryParser<SemanticTrajectory> setDelimiter(String delimiter) {
        super.setDelimiter(delimiter);
        return this;
    }

    @Override
    public AbstractTrajectoryParser<SemanticTrajectory> setLineTerminators(char[][] lineTerminators) {
        super.setLineTerminators(lineTerminators);
        return this;
    }

    @Override
    public AbstractTrajectoryParser<SemanticTrajectory> setnLinesToSkip(int nLinesToSkip) {
        super.setnLinesToSkip(nLinesToSkip);
        return this;
    }

    @Override
    public AbstractTrajectoryParser<SemanticTrajectory> setProgressListener(Consumer<Double> progressListener) {
        super.setProgressListener(progressListener);
        return this;
    }
}
