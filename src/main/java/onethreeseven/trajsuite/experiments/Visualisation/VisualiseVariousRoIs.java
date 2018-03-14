package onethreeseven.trajsuite.experiments.Visualisation;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.render.GlobeAnnotation;
import javafx.stage.Stage;
import onethreeseven.common.util.NDUtil;
import onethreeseven.datastructures.graphics.TrajectoryGraphic;
import onethreeseven.datastructures.model.Trajectory;
import onethreeseven.datastructures.util.DataGeneratorUtil;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.roi.algorithm.HybridRoIs;
import onethreeseven.roi.algorithm.SlopeRoIs;
import onethreeseven.roi.algorithm.ThresholdRoIs;
import onethreeseven.roi.algorithm.UniformRoIs;
import onethreeseven.roi.graphics.RoIGraphic;
import onethreeseven.roi.model.MiningSpaceFactory;
import onethreeseven.roi.model.RectangularRoI;
import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIGrid;
import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.util.ViewChanger;
import onethreeseven.trajsuite.core.view.AbstractWWFxApplication;
import onethreeseven.common.util.ColorUtil;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.transaction.AddEntitiesTransaction;
import onethreeseven.trajsuitePlugin.util.IdGenerator;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Random;


/**
 * Generates some mock trajectories and then finds RoIs resolve them
 * using various algorithms within TrajSuite.
 * @author Luke Bermingham
 */
public class VisualiseVariousRoIs extends AbstractWWFxApplication {

    private static final TrajSuiteProgram app = (TrajSuiteProgram) BaseTrajSuiteProgram.getInstance();

    @Override
    protected TrajSuiteProgram preStart(Stage stage) {
        return app;
    }

    @Override
    public String getTitle() {
        return "Find Mock RoIs";
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
        ViewChanger.changeToFlatGrid(wwd, 25);

        /*
         * RoI stuff
         */

        //make the trajectories in "L" shape
        double[][] ptPool = new double[][]{
                new double[]{0, 0, 0},
                new double[]{3, 0, 0},
                new double[]{6, 0, 0},
                new double[]{6, 3, 0},
                new double[]{12, 3, 0}
        };


        LayerList ll = wwd.getModel().getLayers();
        AnnotationLayer annotationLayer = new AnnotationLayer();
        ll.add(annotationLayer);

        int seed = 137;

        for (int i = 0; i < 4; i++) {
            //make some pts
            NDUtil.translatePts(ptPool, new double[]{15, 0, 0});
            //get avg position
            double[] avg = NDUtil.averagePt(ptPool);
            Position labelPos = wwd.getModel().getGlobe().computePositionFromPoint(new Vec4(avg[0], avg[1], 0));

            //trajectories
            Map<String, Trajectory> trajectories = DataGeneratorUtil.generateDensitySlopingTrajectoriesFrom(ptPool, 10);
            //perturb trajectories
            Random random = new Random(seed);
            for (Trajectory trajectory : trajectories.values()) {
                for (int j = 0; j < trajectory.size(); j++) {
                    double[] entry = trajectory.get(j);
                    for (int n = 0; n < entry.length; n++) {
                        entry[n] = entry[n] * (random.nextDouble() * 0.2);
                    }
                }
            }

            ptPool = NDUtil.copyPts(ptPool);
            //roi mining params
            int minDensity = 3;
            RoIGrid roiGrid = MiningSpaceFactory.createGrid(trajectories, new int[]{13, 4, 1}, 0);
            //roi mining
            Collection<RoI> rois;
            switch (i) {
                case 0:
                    rois = new ThresholdRoIs().run(roiGrid, minDensity);
                    annotationLayer.addAnnotation(new GlobeAnnotation("Threshold", labelPos));
                    break;
                case 1:
                    rois = new UniformRoIs().run(roiGrid, minDensity);
                    annotationLayer.addAnnotation(new GlobeAnnotation("Uniform", labelPos));
                    break;
                case 2:
                    rois = new SlopeRoIs().run(roiGrid, minDensity);
                    annotationLayer.addAnnotation(new GlobeAnnotation("Slope", labelPos));
                    break;
                case 3:
                default:
                    rois = new HybridRoIs().run(roiGrid, minDensity);
                    annotationLayer.addAnnotation(new GlobeAnnotation("Hybrid", labelPos));
                    break;
            }

            AbstractGeographicProjection projection = new ProjectionEquirectangular();

            //add rois
            {
                AddEntitiesTransaction transaction = new AddEntitiesTransaction();
                String roiLayer = "RoIs_" + i;

                ArrayList<Integer> densities = new ArrayList<>();
                for (RoI roi : rois) {
                    densities.add((int) roi.getDensity());
                }

                final int minObservedDensity = densities.stream().min(Integer::compareTo).get();
                final int maxObservedDensity = densities.stream().max(Integer::compareTo).get();

                for (RoI roi : rois) {
                    RectangularRoI rectangularRoI = new RectangularRoI(roiGrid, roi, projection);
                    RoIGraphic graphic = new RoIGraphic(rectangularRoI, minObservedDensity, maxObservedDensity);
                    transaction.add(roiLayer, IdGenerator.nextId(), rectangularRoI, graphic);
                }
                app.getLayers().process(transaction);
            }

            //add trajs
            {
                //render trajectories too
                Color[] colors = ColorUtil.generateNColors(trajectories.size());
                String layername = "Trajectories";

                AddEntitiesTransaction transaction = new AddEntitiesTransaction();

                int count = 0;
                for (Map.Entry<String, Trajectory> entry : trajectories.entrySet()) {
                    TrajectoryGraphic graphics = new TrajectoryGraphic(entry.getValue());
                    transaction.add(layername, entry.getKey(), entry.getValue(), graphics);
                    graphics.drawOnTop.setValue(true);
                    graphics.fallbackColor.setValue(colors[count]);
                    count++;
                }

                app.getLayers().process(transaction);
            }



        }
    }
}
