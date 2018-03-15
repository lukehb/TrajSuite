package onethreeseven.trajsuite.core.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * A dialog that shows a progress bar and some information.
 * @author Luke Bermingham
 */
public class ProgressDialog {

    private static final String percentageFmt = " %.2f%% ";
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final Label statusLabel;
    private final Stage stage;

    public ProgressDialog(boolean indeterminate) {
        this.stage = new Stage(StageStyle.UTILITY);
        this.stage.initModality(Modality.WINDOW_MODAL);
        this.stage.setAlwaysOnTop(true);
        progressBar = new ProgressBar();
        progressLabel = new Label();
        statusLabel = new Label();
        this.stage.setScene(new Scene(createUi(indeterminate), 400, 200));
    }

    public void setTitle(String title) {
        stage.setTitle(title);
    }

    public <T> void run(ProgressDialogTask<T> task, Consumer<T> callback) {
        this.stage.show();
        this.stage.centerOnScreen();

        //status listener
        task.status.addListener((observable, oldValue, newValue) ->
                Platform.runLater(() -> statusLabel.setText(newValue)));
        //progress listener
        task.progress.addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
            progressBar.setProgress(newValue.doubleValue());
            progressLabel.setText(String.format(percentageFmt, newValue.doubleValue() * 100));
        }));

        CompletableFuture.supplyAsync(task::call).handle((BiFunction<T, Throwable, Void>) (t, throwable) -> {

            //success
            if(throwable == null){
                callback.accept(t);
                //close the dialog ui
                Platform.runLater(ProgressDialog.this::close);
            }
            //failure
            else{
                throwable.printStackTrace();
            }
            return null;
        });

    }

    private void close() {
        Platform.runLater(() -> {
            stage.close();
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
        });
    }

    private Parent createUi(boolean indeterminate) {
        //set progress bar type
        if (!indeterminate) {
            progressBar.setProgress(0);
        } else {
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        }

        Font textFont = Font.font(18);
        progressLabel.setFont(textFont);
        statusLabel.setFont(textFont);

        //setup ui containers
        VBox parent = new VBox(20, statusLabel, progressBar, progressLabel);
        parent.setPadding(new Insets(10));
        parent.setAlignment(Pos.TOP_CENTER);
        //make progress bar width grow with dialog
        progressBar.prefWidthProperty().bind(parent.widthProperty().subtract(20));
        progressBar.setPrefHeight(40);
        HBox.setHgrow(parent, Priority.ALWAYS);

        return parent;
    }


}
