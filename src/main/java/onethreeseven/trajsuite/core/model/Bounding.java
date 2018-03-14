package onethreeseven.trajsuite.core.model;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.*;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;

import java.util.Arrays;

/**
 * Specified that this object is bounding, and therefore, must provide its bounds.
 * @author Luke Bermingham
 */
public interface Bounding extends Extent {

    /**
     * @return bounds in the format { {min,max}, {min,max}, {min,max} }
     */
    double[][] getBounds();

    /**
     * @param ordinate the dimension index
     * @return The minimum value of this dimension
     */
    default double getMin(int ordinate) {
        return getBounds()[ordinate][0];
    }

    /**
     * @param ordinate the dimension index
     * @return The maximum value of this dimension
     */
    default double getMax(int ordinate) {
        return getBounds()[ordinate][1];
    }

    /**
     * @return the number of dimensions as specified by the bounds
     */
    default int nDimensions() {
        return getBounds().length;
    }

    default double[] getCenterArr() {
        return BoundsUtil.getCenter(getBounds());
    }

    @Override
    default Vec4 getCenter() {
        double[] center = getCenterArr();
        return Vec4.fromDoubleArray(center, 0, center.length);
    }

    @Override
    default double getDiameter() {
        double max = 0;
        for (double dim : getCenterArr()) {
            if(dim > max){
                max = dim;
            }
        }
        return max;
    }

    @Override
    default double getRadius() {
        return getDiameter() / 2;
    }

    /**
     * Get the corners of the bounds
     *
     * @return if it were a 2d, bounds corners like { {min, min} , {max, max}, {max, min}, {min, max} }
     */
    default double[][] getCorners() {
        return BoundsUtil.getCorners(getBounds());
    }

    default Vec4[] getCornerVecs() {
        double[][] corners = getCorners();
        Vec4[] vecs = new Vec4[corners.length];
        int nDimensions = nDimensions();
        for (int i = 0; i < corners.length; i++) {
            vecs[i] = Vec4.fromDoubleArray(corners[i], 0, nDimensions);
        }
        return vecs;
    }

    /**
     * @return a 3d box using the given bounds
     */
    default Box getBox() {
        return Box.computeBoundingBox(Arrays.asList(getCornerVecs()));
    }

    @Override
    default boolean intersects(Frustum frustum) {
        return getBox().intersects(frustum);
    }

    @Override
    default Intersection[] intersect(Line line) {
        return getBox().intersect(line);
    }

    @Override
    default boolean intersects(Line line) {
        return getBox().intersects(line);
    }

    @Override
    default boolean intersects(Plane plane) {
        return getBox().intersects(plane);
    }

    @Override
    default double getEffectiveRadius(Plane plane) {
        return getBox().getEffectiveRadius(plane);
    }

    @Override
    default double getProjectedArea(View view) {
        return getBox().getProjectedArea(view);
    }
}
