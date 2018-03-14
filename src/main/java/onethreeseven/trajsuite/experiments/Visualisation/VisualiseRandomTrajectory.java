package onethreeseven.trajsuite.experiments.Visualisation;

import javafx.stage.Stage;
import onethreeseven.datastructures.algorithm.TrajectoryDragonCurve;
import onethreeseven.datastructures.graphics.TrajectoryGraphic;
import onethreeseven.datastructures.model.Trajectory;
import onethreeseven.datastructures.util.DataGeneratorUtil;
import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.view.AbstractWWFxApplication;
import onethreeseven.common.util.ColorUtil;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.transaction.AddEntitiesTransaction;

import java.awt.*;
import java.util.Map;


/**
 * Loading points in world wind.
 * @author Luke Bermingham
 */
public class VisualiseRandomTrajectory extends AbstractWWFxApplication {

    private static final TrajSuiteProgram program = (TrajSuiteProgram) BaseTrajSuiteProgram.getInstance();

    @Override
    protected TrajSuiteProgram preStart(Stage stage) {
        //make the trajectories
        Map<String, Trajectory> trajectories = DataGeneratorUtil.generateCurvyTrajectories(
                new TrajectoryDragonCurve(), 5);

        Color[] colors = ColorUtil.generateNColors(trajectories.size());

        AddEntitiesTransaction transaction = new AddEntitiesTransaction();
        String layername = "Trajs";

        //add trajs
        int i = 0;
        for (Trajectory trajectory : trajectories.values()) {
            TrajectoryGraphic graphic = new TrajectoryGraphic(trajectory);
            graphic.fallbackColor.setValue(colors[i]);
            transaction.add(layername, String.valueOf(i), trajectory, graphic);
        }

        program.getLayers().process(transaction);

        return program;
    }

    @Override
    public String getTitle() {
        return "Points Test";
    }

    @Override
    public int getStartWidth() {
        return 1000;
    }

    @Override
    public int getStartHeight() {
        return 800;
    }



}
