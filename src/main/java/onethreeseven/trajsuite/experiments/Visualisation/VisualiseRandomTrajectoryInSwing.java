package onethreeseven.trajsuite.experiments.Visualisation;

import onethreeseven.datastructures.algorithm.TrajectoryDragonCurve;
import onethreeseven.datastructures.model.Trajectory;
import onethreeseven.datastructures.util.DataGeneratorUtil;
import onethreeseven.trajsuite.experiments.Data.TrajectoryGeneratorFrame;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Random;

/**
 * Experiment that shows how the parameters in the trajectory dragon curve affect
 * trajectory generation.
 * @author Luke Bermingham
 */
public class VisualiseRandomTrajectoryInSwing {


    public static void main(String[] args) {

        final Random random = new Random();
        Dimension screenDims = Toolkit.getDefaultToolkit().getScreenSize();
        final double[][] desiredBounds = new double[][]{
                new double[]{0, screenDims.getWidth()},
                new double[]{0, screenDims.getHeight()}
        };

        TrajectoryDragonCurve algo = new TrajectoryDragonCurve();
        algo.setBounds(desiredBounds);
        algo.setSeed(random.nextLong());

        //make 10 trajectories for this test

        final TrajectoryDrawing drawing = new TrajectoryDrawing(DataGeneratorUtil.generateCurvyTrajectories(algo, 10));

        JDesktopPane testView = new JDesktopPane();

        TrajectoryGeneratorFrame generatorView = new TrajectoryGeneratorFrame(algo, true) {
            @Override
            public void updateTrajectory(TrajectoryDragonCurve algo, int nTrajectories, boolean newSeed) {
                if (newSeed) {
                    algo.setSeed(random.nextLong());
                }
                algo.setBounds(desiredBounds);
                //set trajectories to draw
                drawing.setToDraw(DataGeneratorUtil.generateCurvyTrajectories(algo, nTrajectories));
                drawing.repaint();
            }
        };
        drawing.setVisible(true);
        testView.add(drawing);
        testView.add(generatorView);
        generatorView.toFront();
        testView.setVisible(true);


        JFrame testFrame = new JFrame();
        testFrame.setContentPane(testView);
        testFrame.setVisible(true);
        testFrame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        testFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        generatorView.setVisible(true);
        generatorView.toFront();


    }


    ///////////////TEST///////////////

    private static class TrajectoryDrawing extends JInternalFrame {

        private Map<String, Trajectory> toDraw;

        public TrajectoryDrawing(Map<String, Trajectory> trajectories) {
            super("Trajectory (Dragon Curve) Generator - By Luke Bermingham");
            this.setSize(Toolkit.getDefaultToolkit().getScreenSize());
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            this.toDraw = trajectories;
            this.setVisible(true);
        }

        public void setToDraw(Map<String, Trajectory> toDraw) {
            this.toDraw = toDraw;
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(Color.LIGHT_GRAY);
            Dimension dims = Toolkit.getDefaultToolkit().getScreenSize();
            g.fillRect(0, 0, dims.width, dims.height);
            g.setColor(Color.BLACK);
            for (Trajectory trajectory : toDraw.values()) {
                double[] prevPt = null;
                for (double[] curPt : trajectory) {
                    if (prevPt == null) {
                        prevPt = curPt;
                    } else {
                        g.drawLine(
                                ((Double) prevPt[0]).intValue(),
                                ((Double) prevPt[1]).intValue(),
                                ((Double) curPt[0]).intValue(),
                                ((Double) curPt[1]).intValue());
                        prevPt = curPt;
                    }
                }
            }
        }
    }


}
