package onethreeseven.trajsuite.core.util;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.globes.FlatGlobe;
import gov.nasa.worldwind.globes.projections.ProjectionEquirectangular;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.Earth.BMNGOneImage;
import gov.nasa.worldwind.layers.Earth.NASAWFSPlaceNameLayer;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.wms.WMSTiledImageLayer;
import onethreeseven.common.util.Res;
import onethreeseven.trajsuite.core.view.BasicOrbitViewLimitsFlatGlobe;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Utility for changing between the different viewing types in this application.
 * 3d map globe, 2d grid etc.
 * @author Luke Bermingham
 */
public final class ViewChanger {



    private ViewChanger(){}

    public static void changeToRoundEarth(WorldWindow wwd) {
        Earth earthRound = new Earth();

        wwd.getModel().setGlobe(earthRound);

        BasicOrbitView newView = new BasicOrbitView();

        wwd.setView(newView);
        setupWorldLayers(wwd.getModel().getLayers());

    }

    public static void changeToFlatEarth(WorldWindow wwd) {
        EarthFlat earthFlat = new EarthFlat();
        wwd.getModel().setGlobe(earthFlat);

        BasicOrbitView flatOrbitView = new BasicOrbitView();
        flatOrbitView.setOrbitViewLimits(new BasicOrbitViewLimitsFlatGlobe());
        wwd.setView(flatOrbitView);


        setupWorldLayers(wwd.getModel().getLayers());
    }

    public static void changeToFlatGrid(WorldWindow wwd, int radius) {

        FlatGlobe flatGlobe = new FlatGlobe(radius, radius, 0, new ZeroElevationModel());

        wwd.getModel().setGlobe(flatGlobe);

        flatGlobe.setProjection(new ProjectionEquirectangular());
        // Switch to flat view and update with current position
        View orbitView = wwd.getView();
        BasicOrbitView flatOrbitView = new BasicOrbitView();
        flatOrbitView.setOrbitViewLimits(new BasicOrbitViewLimitsFlatGlobe());

        Angle fov = flatOrbitView.getFieldOfView();
        Position centerPos = Position.fromDegrees(0, 0);
        double zoom = radius * 4 / (fov.tanHalfAngle() * fov.cosHalfAngle());

        flatOrbitView.setCenterPosition(centerPos);
        flatOrbitView.setZoom(zoom);
        flatOrbitView.setHeading(orbitView.getHeading());
        flatOrbitView.setPitch(orbitView.getPitch());
        wwd.setView(flatOrbitView);

        //setup the layers, has to happen after we have set up the view
        setupGridLayers(wwd.getModel().getLayers(), zoom);
    }


    @SuppressWarnings("unchecked")
    private static <T extends Layer> void enableLayer(LayerList ll, Class<T> type, Consumer<T> callback) {
        Collection<Layer> layers = ll.getLayersByClass(type);
        T layer = null;
        if (layers.isEmpty()) {
            try {
                layer = type.getDeclaredConstructor().newInstance();
                ll.add(layer);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            for (Layer someLayer : layers) {
                if (type.equals(someLayer.getClass())) {
                    layer = (T) someLayer;
                    layer.setEnabled(true);
                    break;
                }
            }
        }
        callback.accept(layer);
    }

    public static void setupWorldLayers(LayerList ll) {
        //turn off all flat grid layers
        for (Layer layer : ll.getLayersByClass(LatLonGraticuleLayer.class)) {
            layer.setEnabled(false);
        }




        File configFile = new Res().getFile("worldwind_layers.xml");

        Object obj = new BasicLayerFactory().createFromConfigSource(configFile, null);

        if (obj != null) {
            Object[] objects = (Object[]) obj;
            if (objects.length > 0) {
                for (Layer layer : (LayerList) objects[0]) {

                    Layer existingLayer = ll.getLayerByName(layer.getName());

                    if (existingLayer == null) {
                        ll.add(layer);
                    } else {
                        ll.remove(existingLayer);
                        ll.add(layer);
                    }
                }
            }
        }
    }

    public static void turnOffWmsLayers(LayerList ll){
        //turn off all wms layers
        for (Layer layer : ll.getLayersByClass(WMSTiledImageLayer.class)) {
            layer.setEnabled(false);
        }
        for (Layer layer : ll.getLayersByClass(BasicTiledImageLayer.class)) {
            layer.setEnabled(false);
        }
        for (Layer layer : ll.getLayersByClass(NASAWFSPlaceNameLayer.class)) {
            layer.setEnabled(false);
        }
        for (Layer layer : ll.getLayersByClass(SkyGradientLayer.class)) {
            layer.setEnabled(false);
        }
        for (Layer layer : ll.getLayersByClass(BMNGOneImage.class)) {
            layer.setEnabled(false);
        }
        for (Layer layer : ll.getLayersByClass(WorldMapLayer.class)) {
            layer.setEnabled(false);
        }
        for (Layer layer : ll.getLayersByClass(StarsLayer.class)) {
            layer.setEnabled(false);
        }
    }

    private static void setupGridLayers(LayerList ll, double altitude) {

        turnOffWmsLayers(ll);
        enableLayer(ll, CompassLayer.class, compassLayer -> {
        });
        enableLayer(ll, ScalebarLayer.class, scalebarLayer -> {
        });
        enableLayer(ll, LatLonGraticuleLayer.class, latlonGridLayer -> {
            //turn off lat/lon labels and make grid lines go resolve white to black
            latlonGridLayer.setDrawLabels(false, LatLonGraticuleLayer.GRATICULE_LATLON_LEVEL_0);
            latlonGridLayer.setGraticuleLineColor(Color.DARK_GRAY, LatLonGraticuleLayer.GRATICULE_LATLON_LEVEL_0);
            latlonGridLayer.setDrawLabels(false, LatLonGraticuleLayer.GRATICULE_LATLON_LEVEL_1);
            latlonGridLayer.setGraticuleLineColor(Color.BLACK, LatLonGraticuleLayer.GRATICULE_LATLON_LEVEL_1);
            latlonGridLayer.setDrawLabels(false, LatLonGraticuleLayer.GRATICULE_LATLON_LEVEL_2);
            latlonGridLayer.setGraticuleLineColor(Color.BLACK, LatLonGraticuleLayer.GRATICULE_LATLON_LEVEL_2);
            latlonGridLayer.setDrawLabels(false, LatLonGraticuleLayer.GRATICULE_LATLON_LEVEL_3);
            latlonGridLayer.setGraticuleLineColor(Color.BLACK, LatLonGraticuleLayer.GRATICULE_LATLON_LEVEL_3);
            latlonGridLayer.setDrawLabels(false, LatLonGraticuleLayer.GRATICULE_LATLON_LEVEL_4);
            latlonGridLayer.setGraticuleLineColor(Color.BLACK, LatLonGraticuleLayer.GRATICULE_LATLON_LEVEL_4);
            latlonGridLayer.setDrawLabels(false, LatLonGraticuleLayer.GRATICULE_LATLON_LEVEL_5);
            latlonGridLayer.setGraticuleLineColor(Color.BLACK, LatLonGraticuleLayer.GRATICULE_LATLON_LEVEL_5);
        });
    }


}
