package onethreeseven.trajsuite.experiments.WorldWind;


import javafx.stage.Stage;
import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.util.WWExtrasUtil;
import onethreeseven.trajsuite.core.view.AbstractWWFxApplication;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;

/**
 * Testing some methods to get an accurate view sector
 * Created by Luke.
 */
public class OutputViewSectorWorldWind extends AbstractWWFxApplication {

    private static final TrajSuiteProgram app = (TrajSuiteProgram) BaseTrajSuiteProgram.getInstance();

    @Override
    protected TrajSuiteProgram preStart(Stage stage) {
        return app;
    }

    @Override
    public String getTitle() {
        return "View Sector Experiment";
    }

    @Override
    public int getStartWidth() {
        return 1000;
    }

    @Override
    public int getStartHeight() {
        return 800;
    }

    private void setupViewListener(WorldWindow wwd) {
        wwd.getView().addPropertyChangeListener(AVKey.VIEW, evt ->
                System.out.println(WWExtrasUtil.getVisibleSector(wwd.getView())));
    }

}
