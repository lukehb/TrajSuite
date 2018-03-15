package onethreeseven.trajsuite.core.view.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;

import java.awt.*;
import java.net.URI;

/**
 * Controller for about view
 * @author Luke Bermingham
 */
public class AboutViewController {


    @FXML
    public Hyperlink docsLink;

    @FXML
    public void onDocsLinkClicked(ActionEvent actionEvent) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                URI uri = new URI(docsLink.getText());
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
