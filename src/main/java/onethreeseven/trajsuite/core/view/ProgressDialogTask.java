package onethreeseven.trajsuite.core.view;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * A way to report the progress of a given task.
 * @see ProgressDialog Relies on this class to report progress.
 * @author Luke Bermingham
 */
public abstract class ProgressDialogTask<T> implements Callable<T>, Consumer<Double> {

    final DoubleProperty progress = new SimpleDoubleProperty();
    final StringProperty status = new SimpleStringProperty();

    @Override
    public final void accept(Double progress) {
        this.progress.setValue(progress);
    }

    public final void describe(String status) {
        this.status.setValue(status);
    }

    @Override
    public abstract T call();

}
