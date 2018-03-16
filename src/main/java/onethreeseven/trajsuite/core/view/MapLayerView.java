package onethreeseven.trajsuite.core.view;

import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.mercator.BasicMercatorTiledImageLayer;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Widget to change map layer rendering
 * @author Luke Bermingham
 */
public class MapLayerView extends VBox {

    public MapLayerView(LayerList ll) {
        populateGridWithMapLayers(ll);
    }

    private void populateGridWithMapLayers(LayerList ll){

        this.getChildren().clear();

        this.getChildren().add(new Label("Map layers:"));

        for (Layer layer : ll) {

            if(layer instanceof BasicTiledImageLayer || layer instanceof BasicMercatorTiledImageLayer){

                //make checkbox that toggles the layer
                CheckBox isRendering = new CheckBox(layer.getName());
                isRendering.setSelected(layer.isEnabled());

                isRendering.selectedProperty().addListener((observable, oldValue, newValue) -> layer.setEnabled(newValue));

                this.getChildren().add(isRendering);

            }

        }

    }

}
