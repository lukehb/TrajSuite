package onethreeseven.trajsuite.core.graphics;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import onethreeseven.trajsuitePlugin.graphics.CirclePrefab;
import onethreeseven.trajsuitePlugin.graphics.GraphicsPrefab;
import onethreeseven.trajsuitePlugin.graphics.LabelPrefab;

import java.awt.*;

/**
 * Turns prefabs into world wind graphics
 * @author Luke Bermingham
 */
public class PrefabToRenderableFactory {


    public static SurfaceText createText(LabelPrefab prefab){
        LatLon ll = LatLon.fromDegrees(prefab.centerLatLon.get()[0], prefab.centerLatLon.get()[1]);
        Position pos = Position.fromLatLon(ll);

        SurfaceText text = new SurfaceText(prefab.label.get(), pos);
        text.setTextSize(100);
        text.setColor(Color.BLACK);
        text.setBackgroundColor(Color.WHITE);
        text.setPriority(100);

        prefab.centerLatLon.addListener((observable, oldValue, newValue) -> {
            LatLon ll1 = LatLon.fromDegrees(prefab.centerLatLon.get()[0], prefab.centerLatLon.get()[1]);
            Position pos1 = Position.fromLatLon(ll1);
            text.moveTo(pos1);
        });

        return text;
    }

    public static AbstractAnnotation createAnnotation(LabelPrefab prefab){

        LatLon ll = LatLon.fromDegrees(prefab.centerLatLon.get()[0], prefab.centerLatLon.get()[1]);
        Position pos = Position.fromLatLon(ll);

        GlobeAnnotation annotation = new GlobeAnnotation(prefab.label.get(), pos);

        //annotation.setHeightInMeter();
        AnnotationAttributes attrs = new AnnotationAttributes();

        if(!prefab.doesScale.get()){
            attrs.setDistanceMinScale(0.2);
            annotation.setHeightInMeter(100);
        }

        //attrs.setScale(0.5);
        attrs.setVisible(true);

        //add listeners

        //color
        prefab.color.addListener((observable, oldValue, newValue) -> attrs.setTextColor(newValue));
        //latlon pos
        prefab.centerLatLon.addListener((observable, oldValue, newValue) -> {
            LatLon ll1 = LatLon.fromDegrees(prefab.centerLatLon.get()[0], prefab.centerLatLon.get()[1]);
            Position pos1 = Position.fromLatLon(ll1);
            annotation.moveTo(pos1);
        });

        annotation.setAttributes(attrs);

        return annotation;
    }

    public static AbstractSurfaceShape createAnnotation(GraphicsPrefab prefab){

        LatLon ll = LatLon.fromDegrees(prefab.centerLatLon.get()[0], prefab.centerLatLon.get()[1]);

        ShapeAttributes attr = new BasicShapeAttributes();
        attr.setDrawInterior(true);
        attr.setDrawOutline(true);
        attr.setOutlineWidth(3);
        attr.setInteriorOpacity(0.5);

        updateShapeColor(attr, prefab.color.get());

        //when color changes on prefab, update the shape attrs
        prefab.color.addListener((observable, oldValue, newValue) -> updateShapeColor(attr, newValue));


        if(prefab instanceof CirclePrefab){
            CirclePrefab circlePrefab = ((CirclePrefab) prefab);
            SurfaceCircle circle = new SurfaceCircle(attr, ll, circlePrefab.radiusMetres.get());

            circle.setVisible(true);

            //add change listener for radius
            circlePrefab.radiusMetres.addListener((observable, oldValue, newValue) -> circle.setRadius(newValue.doubleValue()));
            //add change listener for position
            circlePrefab.centerLatLon.addListener((observable, oldValue, newValue) -> {
                LatLon ll1 = LatLon.fromDegrees(newValue[0], newValue[1]);
                circle.moveTo(new Position(ll1, 0));
            });

            return circle;
        }

        throw new UnsupportedOperationException("Shape factory does not know what kind of " +
                "surface shape to make for a: " + prefab);
    }

    private static void updateShapeColor(ShapeAttributes attrs, Color interiorColor){

        Material interiorMat = new Material(interiorColor);
        Color outlineColor = interiorColor.darker();
        Material outlineMat = new Material(outlineColor);
        attrs.setOutlineMaterial(outlineMat);
        attrs.setInteriorMaterial(interiorMat);

    }


}
