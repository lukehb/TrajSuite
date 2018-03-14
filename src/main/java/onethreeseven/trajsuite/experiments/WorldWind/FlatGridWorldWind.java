package onethreeseven.trajsuite.experiments.WorldWind;

import javafx.stage.Stage;
import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.util.ViewChanger;
import onethreeseven.trajsuite.core.view.AbstractWWFxApplication;
import gov.nasa.worldwind.WorldWindow;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;

/**
 * Loading world wind as a flat grid.
 * @author Luke Bermingham
 */
public class FlatGridWorldWind extends AbstractWWFxApplication {

    private static final TrajSuiteProgram layers = (TrajSuiteProgram) BaseTrajSuiteProgram.getInstance();

    @Override
    protected TrajSuiteProgram preStart(Stage stage) {


        return layers;
    }

    @Override
    public String getTitle() {
        return "Flat grid worldwind";
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
        ViewChanger.changeToFlatGrid(wwd, 1000);
    }
}
