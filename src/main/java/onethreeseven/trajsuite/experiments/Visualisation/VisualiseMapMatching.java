package onethreeseven.trajsuite.experiments.Visualisation;

import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.storage.index.QueryResult;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Earth.OSMMapnikLayer;
import javafx.stage.Stage;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.model.STTrajectory;
import onethreeseven.datastructures.model.SpatialTrajectory;
import onethreeseven.geo.projection.ProjectionMercator;
import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.model.RenderableEntity;
import onethreeseven.trajsuite.core.model.Tuple;
import onethreeseven.trajsuite.core.view.AbstractWWFxApplication;
import onethreeseven.trajsuite.osm.model.GHGraphWrapper;
import onethreeseven.trajsuite.osm.model.GPXFileWrapper;
import onethreeseven.trajsuite.osm.model.GraphHopperDAO;
import onethreeseven.trajsuitePlugin.graphics.GraphicsPayload;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.model.WrappedEntity;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Quick test to visualise a gpx trace being map matched to OSM
 * data and then being visualised in world wind.
 * @author Luke Bermingham
 */
public class VisualiseMapMatching extends AbstractWWFxApplication {

    private static final TrajSuiteProgram layers = (TrajSuiteProgram) BaseTrajSuiteProgram.getInstance();

    private final File gpxFile =
            new File(FileUtil.makeAppDir("gpx"), "366_tdrive.gpx");

    private final File extract =
            new File(FileUtil.makeAppDir("gpx"), "beijing_china.osm.pbf");

    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    protected TrajSuiteProgram preStart(Stage stage) {
        return layers;
    }

    @Override
    public String getTitle() {
        return "Map matching";
    }

    @Override
    public int getStartWidth() {
        return 1000;
    }

    //some files needed for map matching

    @Override
    public int getStartHeight() {
        return 800;
    }

    @Override
    protected void onViewReady(WorldWindow wwd) {
        //add osm layer
        wwd.getModel().getLayers().add(new OSMMapnikLayer());


        final GraphHopperDAO dao = new GraphHopperDAO(extract);
        layers.getLayers().add(new GHGraphWrapper(dao, new ProjectionMercator()));

        wwd.getInputHandler().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Position pos = wwd.getCurrentPosition();
                if(pos != null){
                    List<QueryResult> qrs = dao.queryAt(pos.getLatitude().degrees, pos.getLongitude().degrees, 200);
                    for (QueryResult qr : qrs) {
                        System.out.println("Edge of " + qr.getClosestEdge().getBaseNode()
                                + "-" + qr.getClosestEdge().getAdjNode() + " getEuclideanDistance:" + qr.getQueryDistance());
                    }
                    System.out.println("\n");
                }
            }
        });

        CompletableFuture<Tuple<List<EdgeMatch>, STTrajectory>> trajRes = mapMatch(dao, wwd);

        trajRes.handle((listSTTrajectoryTuple, throwable) -> {

            //success
            if(throwable == null){
                System.out.println("Loaded gpx as a trajectory.");

                //setup stuff
                Globe globe = wwd.getModel().getGlobe();

                //add trajectory
                SpatialTrajectory matchedTrail = dao.matchesToTrajectory(listSTTrajectoryTuple.getValue1(), globe);
                layers.getLayers().add(matchedTrail);

                WrappedEntity ptsEntity = layers.getLayers().add(matchedTrail);
                if(ptsEntity instanceof RenderableEntity){
                    GraphicsPayload payload = ((RenderableEntity) ptsEntity).getPayload();
                    payload.fallbackColor.setValue(Color.GREEN);
                    payload.pointOrLineSize.setValue(10);
                    payload.smoothPoints.setValue(true);
                    //make points draw on top of trajectory lines (easier to see)
                    payload.drawOnTop.set(true);
                }
            }
            //failure
            else{
                throwable.printStackTrace();
            }

            return null;
        });
    }

    private CompletableFuture<Tuple<List<EdgeMatch>, STTrajectory>> mapMatch(final GraphHopperDAO dao, WorldWindow wwd) {
        return CompletableFuture.supplyAsync(() -> {
            GPXFileWrapper trail = new GPXFileWrapper(gpxFile);
            MatchResult mr = dao.mapMatch(trail, true);
            STTrajectory traj = trail.toTrajectory();
            return new Tuple<>(mr.getEdgeMatches(), traj);
        }, exec);
    }


}
