package onethreeseven.trajsuite.core;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;
import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.settings.TrajSuiteSettings;
import onethreeseven.trajsuite.core.view.AbstractWWFxApplication;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;
import onethreeseven.trajsuitePlugin.transaction.AddBoundingEntityUnit;
import onethreeseven.trajsuitePlugin.transaction.AddEntitiesTransaction;
import onethreeseven.trajsuitePlugin.transaction.AddEntityUnit;
import java.awt.*;
import java.util.Collection;

/**
 * The entry point for TrajSuite.
 *
 * @author Luke Bermingham
 */
public class Main extends AbstractWWFxApplication {

    private static boolean runningHeadless = false;
    private static TrajSuiteProgram program;

    private static final ChangeListener<? super AddEntitiesTransaction> onEntitiesAdded = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends AddEntitiesTransaction> observable, AddEntitiesTransaction oldValue, AddEntitiesTransaction newValue) {
            Collection<AddEntityUnit> units = newValue.getData();
            if (!units.isEmpty()) {
                for (AddEntityUnit unit : units) {
                    if (unit instanceof AddBoundingEntityUnit) {
                        Object model = unit.getModel();
                        if (model instanceof BoundingCoordinates) {
                            try {
                                program.flyTo((BoundingCoordinates) model);
                            } catch (Exception ignore) {
                            }

                        }
                    }
                }
            }
        }
    };

    public static void main(String[] args) {

        //this is the only time we parse CLI args manually
        initialCLIParse(args);

        //launch trajsuite
        program = (TrajSuiteProgram) BaseTrajSuiteProgram.getInstance();

        //run with head, initialise WorldWind
        if(!runningHeadless){
            launch(args);
        }
    }

    private static void initialCLIParse(String[] args){
        for (String arg : args) {
            if(arg.trim().toLowerCase().equals("-headless")){
                runningHeadless = true;
            }
        }
    }


    @Override
    protected TrajSuiteProgram preStart(Stage stage) {
        stage.setMaximized(true);
        return program;
    }


    @Override
    public void stop() throws Exception {
        super.stop();
        program.shutdown();
        Platform.exit();
        System.exit(0);
    }

    @Override
    public String getTitle() {
        return "Trajsuite by Luke Bermingham";
    }

    @Override
    public int getStartWidth() {
        return Toolkit.getDefaultToolkit().getScreenSize().width;
    }

    @Override
    public int getStartHeight() {
        return Toolkit.getDefaultToolkit().getScreenSize().height;
    }

    private long lastTimeLocationStored = 0;
    private static final long locationStoredCooldownMs = 5000;

    protected void setupLocationHistory(WorldWindow wwd){
        wwd.getView().addPropertyChangeListener(AVKey.VIEW, evt -> {
            //check if it has been long enough between storing locations
            long timeDiff = System.currentTimeMillis() - lastTimeLocationStored;
            if(timeDiff > locationStoredCooldownMs){
                Position pos = wwd.getView().getEyePosition();
                TrajSuiteSettings.LAST_POSITION.changeSetting(pos);
                lastTimeLocationStored = System.currentTimeMillis();
            }
        });

    }

    @Override
    protected void onViewReady(WorldWindow wwd) {
        super.onViewReady(wwd);

        setupLocationHistory(wwd);

        //fly to location when new entities added
        program.getLayers().addEntitiesTransactionProperty.addListener(onEntitiesAdded);

        //load some trajectories
        //program.getCLI().doCommand(new String[]{"gt", "-nt", String.valueOf(2), "-ne", String.valueOf(10)});





    }
}
