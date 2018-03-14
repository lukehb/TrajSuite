package onethreeseven.trajsuite.core.graphics;

/**
 * Some static settings used for graphics objects.
 * @author Luke Bermingham
 */
public final class GraphicsSettings {

    /**
     * Whether we should assign a unique color per renderable.
     */
    private static boolean uniqueColorPerRenderable = false;

    /**
     * Default a bit above everest height (meters)
     */
    private static double defaultElevation = 10000;

    /**
     * When some graphics draw they use glPointSize().
     * Change this value to change the size of the points in pixels.
     */
    private static double preferredPointSize = 5;

    /**
     * When some graphics draw they use GL.GL_POINTS.
     * In this rendering mode points can be drawn as squares
     * or a smooth dots. The smooth dots are more expensive but look better.
     */
    private static boolean useSmoothPoints = false;

    /**
     * When some graphics draw they use glLineWidth().
     * @see GLTrajectory is an example of such a class.
     * Change this value to change the thickness of the lines in pixels.
     */
    private static double preferredLineWidth = 3;

    /**
     * A trajectory can be drawn as a series of arrows (showing direction)
     * or as a series of line segments. The default is line segments.
     */
    private static boolean drawTrajectoryAsPoints = false;

    private static boolean scaleTrajectoryLines = true;

    public static boolean getDrawTrajectoryAsPoints() {
        return drawTrajectoryAsPoints;
    }

    public static void setDrawTrajectoryAsPoints(boolean drawTrajectoryAsPoints) {
        GraphicsSettings.drawTrajectoryAsPoints = drawTrajectoryAsPoints;
    }

    public static boolean doScaleTrajectoryLines() {
        return scaleTrajectoryLines;
    }

    public static void setScaleTrajectoryLines(boolean scaleTrajectoryLines) {
        GraphicsSettings.scaleTrajectoryLines = scaleTrajectoryLines;
    }

    public static double getDefaultElevation() {
        return defaultElevation;
    }

    public static void setDefaultElevation(double defaultElevation) {
        GraphicsSettings.defaultElevation = defaultElevation;
    }

    public static double getPreferredPointSize() {
        return preferredPointSize;
    }

    public static void setPreferredPointSize(double preferredPointSize) {
        GraphicsSettings.preferredPointSize = preferredPointSize;
    }

    public static double getPreferredLineWidth() {
        return preferredLineWidth;
    }

    public static void setPreferredLineWidth(double preferredLineWidth) {
        GraphicsSettings.preferredLineWidth = preferredLineWidth;
    }

    public static boolean useSmoothPoints() {
        return useSmoothPoints;
    }

    public static void setUseSmoothPoints(boolean smoothPoints) {
        GraphicsSettings.useSmoothPoints = smoothPoints;
    }

    public static boolean isUniqueColorPerRenderable() {
        return uniqueColorPerRenderable;
    }

    public static void setUniqueColorPerRenderable(boolean uniqueColorPerRenderable) {
        GraphicsSettings.uniqueColorPerRenderable = uniqueColorPerRenderable;
    }
}
