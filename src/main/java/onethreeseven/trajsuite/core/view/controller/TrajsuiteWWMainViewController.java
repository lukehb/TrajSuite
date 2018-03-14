package onethreeseven.trajsuite.core.view.controller;


import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.globes.FlatGlobe;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingNode;
import javafx.stage.Stage;
import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.settings.TrajSuiteSettings;
import onethreeseven.trajsuite.core.util.ViewChanger;
import onethreeseven.trajsuite.core.view.CheapAWTInputHandler;
import onethreeseven.trajsuitePlugin.view.controller.MainViewController;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Controller for the main view in TrajSuite.
 * @author Luke Bermingham
 */
public class TrajsuiteWWMainViewController extends MainViewController {


    private final ObjectProperty<WorldWindow> wwdView =
            new SimpleObjectProperty<>(null);

    public TrajsuiteWWMainViewController(TrajSuiteProgram program, Stage stage){
        super(program, stage);
    }

    protected TrajSuiteProgram getProgram(){
        return (TrajSuiteProgram) program;
    }

    @Override
    protected void initialize(){
        super.initialize();

        //load worldwind into center of border pane
        final SwingNode swingNode = new SwingNode();
        createSwingContent(swingNode);
        topLevelPane.setCenter(swingNode);
    }

    private void createSwingContent(final SwingNode swingNode) {

        //try get last latlon
        Position startupPos = TrajSuiteSettings.LAST_POSITION.getSetting();
        Configuration.setValue(AVKey.INITIAL_LATITUDE, startupPos.latitude.degrees);
        Configuration.setValue(AVKey.INITIAL_LONGITUDE, startupPos.longitude.degrees);
        Configuration.setValue(AVKey.INITIAL_ALTITUDE, startupPos.elevation);

        WorldWindowGLJPanel wwd = new WorldWindowGLJPanel();
        wwd.setInputHandler(new CheapAWTInputHandler());

        //wwd.setPreferredSize(preferredWwdSize);
        swingNode.setContent(wwd);

        ZeroElevationModel zeroElevationModel = new ZeroElevationModel();
        zeroElevationModel.setNetworkRetrievalEnabled(false);

        FlatGlobe earth = new FlatGlobe(EarthFlat.WGS84_EQUATORIAL_RADIUS, EarthFlat.WGS84_POLAR_RADIUS, EarthFlat.WGS84_ES, zeroElevationModel);

        ViewChanger.setupWorldLayers(getProgram().getLayers().getRenderableLayers());
        BasicModel model = new BasicModel(earth, getProgram().getLayers().getRenderableLayers());
        wwd.setModel(model);

        pollGlobeUntilReady(wwd);


        //when entity changes, re-draw
        getProgram().getLayers().numEditedEntitiesProperty.addListener((observable, oldValue, newValue) -> {
            if(wwd.getView().getGlobe() != null){
                wwd.redraw();
            }
        });

    }

    private void pollGlobeUntilReady(final WorldWindow wwd){

        ThreadFactory factory = r -> new Thread(r, "Poll for WorldWindow.");
        ExecutorService service = Executors.newSingleThreadExecutor(factory);

        CompletableFuture.runAsync(()->{
            while(wwd.getView().getGlobe() == null){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, service).thenRun(()-> {
            getProgram().setWwd(wwd);
            wwdView.setValue(wwd);
        });
    }

    public void addOnViewReadyListener(ChangeListener<? super WorldWindow> listener){
        this.wwdView.addListener(listener);
    }


}
