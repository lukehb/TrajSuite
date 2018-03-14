package onethreeseven.trajsuite.experiments.Visualisation;

import gov.nasa.worldwind.WorldWindow;
import javafx.stage.Stage;
import onethreeseven.datastructures.algorithm.TrajectoryDragonCurve;
import onethreeseven.datastructures.model.Trajectory;
import onethreeseven.datastructures.util.DataGeneratorUtil;
import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.model.RenderableEntity;
import onethreeseven.trajsuite.core.util.ViewChanger;
import onethreeseven.trajsuite.core.view.AbstractWWFxApplication;
import onethreeseven.trajsuitePlugin.graphics.GraphicsPayload;
import onethreeseven.trajsuitePlugin.graphics.RenderingModes;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.model.WrappedEntity;

import java.awt.*;
import java.util.Map;

/**
 * Generate trajectories and then show them in world wind.
 * @author Luke Bermingham
 */
public class VisualiseRandomPoints extends AbstractWWFxApplication {

    private static final TrajSuiteProgram app = (TrajSuiteProgram) BaseTrajSuiteProgram.getInstance();

    @Override
    protected TrajSuiteProgram preStart(Stage stage) {
        return app;
    }

    @Override
    public String getTitle() {
        return "Trajectory as Points Test";
    }

    @Override
    public int getStartWidth() {
        return 1000;
    }

    @Override
    public int getStartHeight() {
        return 800;
    }

    @Override
    protected void onViewReady(WorldWindow wwd) {
        //make a flat grid (unrelated to width)
        ViewChanger.changeToFlatGrid(wwd, 1000);
        //make the trajectories
        Map<String, Trajectory> trajectories = DataGeneratorUtil.generateCurvyTrajectories(new TrajectoryDragonCurve(), 5);

        //add points
        for (Trajectory traj : trajectories.values()) {
            WrappedEntity pointsRenderableEntity = app.getLayers().add(traj);
            if(pointsRenderableEntity instanceof RenderableEntity){

                GraphicsPayload payload = ((RenderableEntity) pointsRenderableEntity).getPayload();

                payload.fallbackColor.setValue(Color.BLUE);

                payload.setRenderingMode(RenderingModes.POINTS);
            }

        }
    }

}
