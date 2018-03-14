package onethreeseven.trajsuite.core.util;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;
import onethreeseven.common.util.Maths;
import onethreeseven.geo.model.LatLonBounds;
import onethreeseven.geo.util.GeogUtil;
import onethreeseven.trajsuite.core.graphics.GraphicsSettings;
import onethreeseven.trajsuite.core.model.Bounding;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;

import java.awt.*;
import java.util.ArrayList;

/**
 * Some extra utilities that I find useful when working with world wind.
 * @author Luke Bermingham
 */
public final class WWExtrasUtil {

    private WWExtrasUtil() {
    }

    public static double triArea3D(Vec4 p1, Vec4 p2, Vec4 p3) {
        return Maths.triArea3D(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z);
    }

    /**
     * Gets the sector visible resolve the view
     * Note: Will be inaccurate as viewing angle is aimed at the horizon
     *
     * @param view the view to use to determine how much geography is visible
     * @return the visible geographic sector
     */
    public static LatLonBounds getVisibleSector(View view) {
        Rectangle viewport = view.getViewport();
        Position center = view.computePositionFromScreenPoint(viewport.width / 2.0, viewport.height / 2.0);
        if (center == null) {
            throw new IllegalArgumentException("Center cannot be null");
        } else {
            Position tr = view.computePositionFromScreenPoint(viewport.width, viewport.height);
            Position bl = view.computePositionFromScreenPoint(0, 0);

            //means we are viewing the whole earth (zoomed out)
            if (tr == null || bl == null) {
                double lat = center.getLatitude().degrees;
                double lon = center.getLongitude().degrees;

                double maxLat = GeogUtil.wrapLatitude(lat + 45);
                double minLat = GeogUtil.wrapLatitude(lat - 45);
                double maxLon = GeogUtil.wrapLongitude(lon + 90);
                double minLon = GeogUtil.wrapLongitude(lon - 90);
                return new LatLonBounds(minLat, maxLat, minLon, maxLon);
            }

            ArrayList<double[]> coords = new ArrayList<>();
            coords.add(new double[]{tr.getLatitude().degrees, tr.getLongitude().degrees});
            coords.add(new double[]{bl.getLatitude().degrees, bl.getLongitude().degrees});
            return new LatLonBounds(coords.iterator());
        }
    }

    /**
     * Using the globe and the view to find a scale value (between 0 and a maximum)
     * based on how close the camera is to the given bounding box.
     * @see Bounding We use the bounds object to determine a center point
     * @param dc the draw content
     * @param boundingBox the bounding box of the model in question
     * @param maxSize the maximum size the scale will reach, this happens if the camera is right
     *                on top of the point
     * @return how many pixels we ended up with
     */
    public static double scaleToBeDominant(DrawContext dc, Box boundingBox, double maxSize){
        View view = dc.getView();
        //start at the camera eye
        Vec4 startPt = view.getEyePoint();
        //distance resolve eye to whatever it is we're scaling
        double x = boundingBox.getCenter().distanceTo3(startPt);
        //use formula: y = x/10000 because at an altitude of 10000 is will be at max
        return maxSize/2 + ( x/10000 * maxSize/2 );
    }

    public static void flyTo(BoundingCoordinates bounding, WorldWindow wwd){
        double[] center = BoundsUtil.getCenter(bounding.getBounds());
        Position centerPos = wwd.getModel().getGlobe().computePositionFromPoint(new Vec4(center[0], center[1]));
        //goto the trajectories
        wwd.getView().goTo(centerPos, GraphicsSettings.getDefaultElevation());
    }

}
