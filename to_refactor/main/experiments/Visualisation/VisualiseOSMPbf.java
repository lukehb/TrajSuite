package onethreeseven.trajsuite.experiments.Visualisation;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import javafx.stage.Stage;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.SpatialTrajectoryParser;
import onethreeseven.datastructures.data.resolver.LatFieldResolver;
import onethreeseven.datastructures.data.resolver.LonFieldResolver;
import onethreeseven.datastructures.data.resolver.SameIdResolver;
import onethreeseven.datastructures.model.SpatialTrajectory;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.graphics.GraphicsSettings;
import onethreeseven.trajsuite.core.model.StamenTonerBaseMapLayer;
import onethreeseven.trajsuite.core.util.ViewChanger;
import onethreeseven.trajsuite.core.view.AbstractWWFxApplication;
import onethreeseven.trajsuite.osm.model.GHGraphWrapper;
import onethreeseven.trajsuite.osm.model.GraphHopperDAO;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Quick test to visualise a OSM pbf file in World Wind.
 * @author Luke Bermingham
 */
public class VisualiseOSMPbf extends AbstractWWFxApplication {

    private static final TrajSuiteProgram app = (TrajSuiteProgram) BaseTrajSuiteProgram.getInstance();

    private static final File trajFile =
            new File(FileUtil.makeAppDir("geolife_merged/beijing"), "001.txt");

    private final File extract =
            new File(FileUtil.makeAppDir("osm"), "china-latest.osm.pbf");

    @Override
    protected TrajSuiteProgram preStart(Stage stage) {
        //1) Load road network
        GraphHopperDAO dao = new GraphHopperDAO(extract);
        GHGraphWrapper ghGraphWrapper = new GHGraphWrapper(dao, new ProjectionEquirectangular());
        app.getLayers().add(ghGraphWrapper);

        //2) Load trajectory
        System.out.println("Begin loading trajectory file.");

        try {
            Map<String, SpatialTrajectory> trajs = new SpatialTrajectoryParser(
                    new SameIdResolver(),
                    new LatFieldResolver(0),
                    new LonFieldResolver(1),
                    new ProjectionEquirectangular(),
                    true)
                    .setDelimiter(",").parse(trajFile);

            System.out.println("Loaded");
            for (SpatialTrajectory traj : trajs.values()) {
                app.getLayers().add(traj);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        return app;
    }

    @Override
    public String getTitle() {
        return "Visualising OSM pbf extract.";
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

        //turn off any wms layers
        ViewChanger.turnOffWmsLayers(wwd.getModel().getLayers());

        //add osm layer
        wwd.getModel().getLayers().add(new StamenTonerBaseMapLayer());

        flyToTrajectory(wwd);

    }

    private void flyToTrajectory(final WorldWindow wwd){

        //fly to centroid of trajectory dataset
        SpatialTrajectory traj = app.getLayers().getFirstEntity(SpatialTrajectory.class).getModel();

        double[] center = BoundsUtil.getCenter(traj.getBounds());

        Position centerPos;

        if(traj.isInCartesianMode()){
            double[] latlon = traj.getProjection().cartesianToGeographic(center);
            centerPos = Position.fromDegrees(latlon[0], latlon[1]);
        }else{
            centerPos = Position.fromDegrees(center[0], center[1]);
        }

        wwd.getView().goTo(centerPos, GraphicsSettings.getDefaultElevation());

    }

}
