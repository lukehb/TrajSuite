package onethreeseven.trajsuite.core.view;

import gov.nasa.worldwind.WorldWindow;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.view.controller.TrajsuiteWWMainViewController;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.view.BasicFxApplication;

import java.io.IOException;
import java.net.URL;

/**
 * Base for creating world wind application using javafx.
 * @author Luke Bermingham
 */
public abstract class AbstractWWFxApplication extends BasicFxApplication {


    protected Stage splashScreen = null;

    @Override
    protected Stage preStageSetup(Stage primaryStage) {
        //hijack the main stage with a splash screen
        splashScreen = makeSplashScreen(primaryStage);
        splashScreen.show();
        //give the application a new stage to use
        return new Stage();
    }

    protected Stage makeSplashScreen(Stage splashStage){
        URL splashFXML = AbstractWWFxApplication.class.getResource("/onethreeseven/trajsuite/core/view/splash.fxml");
        if(splashFXML == null){
            System.err.println("Could not locate splash screen view fxml.");
            return null;
        }

        FXMLLoader loader = new FXMLLoader(splashFXML);
        try {
            Parent view = loader.load();
            Scene splashScene = new Scene(view);
            //splashStage.initOwner(primaryStage);
            splashStage.setScene(splashScene);
            splashStage.setAlwaysOnTop(true);
            splashStage.initStyle(StageStyle.UNDECORATED);
            return splashStage;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected Object initViewController(BaseTrajSuiteProgram program, Stage stage) {
        TrajsuiteWWMainViewController controller = new TrajsuiteWWMainViewController((TrajSuiteProgram) program, stage);
        controller.addOnViewReadyListener((observable, oldValue, newValue) -> {
            if(newValue != null){
                onViewReady(newValue);
            }
        });
        return controller;
    }

    protected void onViewReady(WorldWindow wwd){
        Platform.runLater(()->{
            if(splashScreen != null){
                splashScreen.hide();
            }
        });

    }

}
