package onethreeseven.trajsuite.experiments.Visualisation;

import gov.nasa.worldwind.WorldWindow;
import javafx.stage.Stage;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.SpatialTrajectoryParser;
import onethreeseven.datastructures.data.resolver.IdFieldResolver;
import onethreeseven.datastructures.data.resolver.LatFieldResolver;
import onethreeseven.datastructures.data.resolver.LonFieldResolver;
import onethreeseven.datastructures.model.SpatialTrajectory;
import onethreeseven.geo.model.LatLonBounds;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.graphics.GraphicsSettings;
import onethreeseven.trajsuite.core.model.StamenTonerBaseMapLayer;
import onethreeseven.trajsuite.core.util.ViewChanger;
import onethreeseven.trajsuite.core.util.WWExtrasUtil;
import onethreeseven.trajsuite.core.view.AbstractWWFxApplication;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.model.WrappedEntity;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Load trajectories resolve file and visualise in worldwind.
 * @author Luke Bermingham
 */
public class VisualiseTrajectories extends AbstractWWFxApplication {

    private static final TrajSuiteProgram app = (TrajSuiteProgram) BaseTrajSuiteProgram.getInstance();

    private static final File trajFile = new File(FileUtil.makeAppDir("traj"), "trucks.txt");

    //tdrive
    //1,2008-02-02 15:36:08,116.51172,39.92123

    //buses
    //1359590401000000,13,0,00131005,2013-01-30,3406,CD,0,-6.273923,53.343307,-235,13003,33608,1998,0
    //'Timestamp micro since 1970 01 01 00:00:00 GMT'
    // Line ID
    // 'Direction'
    // Journey Pattern ID'
    // Time Frame'
    // Vehicle Journey ID
    // Operator
    // Congestion [0=no,1=yes]
    // 'Lon WGS84'
    // Lat WGS84'
    // Delay
    // 'Block ID (a section ID of the journey pattern)
    // 'Vehicle ID'
    // Stop ID'
    // At Stop [0=no,1=yes]

    //trucks
    //0862;1;10/09/2002;09:15:59;23.845089;38.018470;486253.80;4207588.10

    private final LatLonBounds dataBounds = LatLonBounds.FULL_SPHERE;

    private Map<String, SpatialTrajectory> loadTrajectories() throws IOException {
        return new SpatialTrajectoryParser(
                new IdFieldResolver(0),
                new LatFieldResolver(5),
                new LonFieldResolver(4),
                new ProjectionEquirectangular(),
                true)
                .setDelimiter(";")
                .parse(trajFile);
    }

    @Override
    protected TrajSuiteProgram preStart(Stage stage) {
        Executor exec = Executors.newSingleThreadExecutor();
        exec.execute(() -> {
            try {
                loadTrajectories().forEach((key, traj) -> {
                    LatLonBounds bounds = traj.calculateGeoBounds();
                    if (dataBounds.contains(bounds)) {
                        app.getLayers().add(traj);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return app;
    }

    @Override
    public String getTitle() {
        return "Visualise Trajectories in WW.";
    }

    @Override
    public int getStartWidth() {
        return 800;
    }

    @Override
    public int getStartHeight() {
        return 600;
    }


    @Override
    public void onViewReady(WorldWindow wwd) {

        //turn off any wms layers
        ViewChanger.turnOffWmsLayers(wwd.getModel().getLayers());
        //add osm layer
        wwd.getModel().getLayers().add(new StamenTonerBaseMapLayer());
        //don't scale trajectories
        GraphicsSettings.setScaleTrajectoryLines(false);

        //get a traj
        WrappedEntity<SpatialTrajectory> traj = app.getLayers().getFirstEntity(SpatialTrajectory.class);
        if(traj != null){
            //fly to it
            WWExtrasUtil.flyTo(traj.getModel(), wwd);
        }

    }
}
