package onethreeseven.trajsuite.experiments.WorldWind;

import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.util.ViewChanger;
import onethreeseven.trajsuite.core.view.AbstractWWFxApplication;
import gov.nasa.worldwind.WorldWindow;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;

/**
 * Change globes: flat, gridded, and spherical.
 * @author Luke Bermingham
 */
public class ChangeGlobesWorldWind extends AbstractWWFxApplication {

    private static final TrajSuiteProgram app = (TrajSuiteProgram) BaseTrajSuiteProgram.getInstance();

    private BorderPane parent;

    @Override
    protected TrajSuiteProgram preStart(Stage stage) {
        return app;
    }

    @Override
    public void start(Stage stage) {
        super.start(stage);
        parent = (BorderPane) stage.getScene().getRoot();
    }

    @Override
    public String getTitle() {
        return "Globe changer";
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
        Platform.runLater(() -> {
            Button gridBtn = new Button("grid");
            gridBtn.setOnAction(event ->
                    ViewChanger.changeToFlatGrid(wwd, 1000));

            Button roundBtn = new Button("round");
            roundBtn.setOnAction(event ->
                    ViewChanger.changeToRoundEarth(wwd));

            Button flatBtn = new Button("flat");
            flatBtn.setOnAction(event ->
                    ViewChanger.changeToFlatEarth(wwd));

            HBox container = new HBox(gridBtn, roundBtn, flatBtn);
            parent.setBottom(container);

        });
    }

}
