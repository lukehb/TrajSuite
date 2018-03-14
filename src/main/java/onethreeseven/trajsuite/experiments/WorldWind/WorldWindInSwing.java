package onethreeseven.trajsuite.experiments.WorldWind;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

import javax.swing.*;
import java.awt.*;


/**
 * Loading world wind in swing example.
 * @author Luke Bermingham
 */
public class WorldWindInSwing extends JFrame
{
    public WorldWindInSwing()
    {
        WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
        wwd.setPreferredSize(new Dimension(1000, 800));
        this.getContentPane().add(wwd, BorderLayout.CENTER);
        wwd.setModel(new BasicModel());
    }

    public static void main(String[] args)
    {

        java.awt.EventQueue.invokeLater(() -> {
            JFrame frame = new WorldWindInSwing();
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        });
    }
}
