package onethreeseven.trajsuite.core.util;

import onethreeseven.trajsuite.core.graphics.GraphicsSettings;
import onethreeseven.trajsuite.core.model.Bounding;
import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * A utility for dealing with objects that have bounds.
 * @see Bounding All things that implement this might use this util.
 * @author Luke Bermingham
 */
public final class WWBoundsUtil {

    private WWBoundsUtil() {
    }

    public static double[][] fromSector(Globe globe, Sector sector) {
        Box box = Sector.computeBoundingBox(globe, 1, sector, 0, GraphicsSettings.getDefaultElevation());
        return fromBox(box);
    }

    public static double[][] fromBox(Box box) {
        Vec4[] corners = box.getCorners();
        return new double[][]{
                new double[]{corners[0].x, corners[2].x},
                new double[]{corners[0].y, corners[4].y},
                new double[]{corners[0].z, corners[1].z}
        };
    }

    /**
     * Finds the 3d bounds given a collections of 3d points
     *
     * @param pts collection of 3d points
     * @return the 3d bounds { {min,max}, {min,max}, {min,max} }
     */
    public static double[][] calculateBounds(Vec4... pts) {
        double[][] bounds = new double[][]{
                new double[]{Double.MAX_VALUE, Double.MIN_VALUE},
                new double[]{Double.MAX_VALUE, Double.MIN_VALUE},
                new double[]{Double.MAX_VALUE, Double.MIN_VALUE}
        };
        final int nDims = 3;
        for (Vec4 pt : pts) {
            double[] values = new double[nDims];
            pt.toArray3(values, 0);
            for (int i = 0; i < nDims; i++) {
                if (values[i] < bounds[i][0]) {
                    bounds[i][0] = values[i];
                }
                if (values[i] > bounds[i][1]) {
                    bounds[i][1] = values[i];
                }
            }
        }
        return bounds;
    }

    public static double[][] calculateBounds(Bounding[] bounds) {
        ArrayList<Bounding> boundsList = new ArrayList<>();
        Collections.addAll(boundsList, bounds);
        return calculateBounds(boundsList);
    }

    public static double[][] calculateFromBoundingCoordinates(Collection<? extends BoundingCoordinates> bounds){
        ArrayList<Bounding> manyBounds = new ArrayList<>();
        for (BoundingCoordinates boundingCoordinates : bounds) {
            manyBounds.add(boundingCoordinates::getBounds);
        }
        return calculateBounds(manyBounds);
    }

    /**
     * Given a collection of bounds, calculate the bounds of the collection
     *
     * @param manyBounds A collection of bounds in the format, { {min, max}, {min, max} }
     * @return the bounds of the collection, like : { {1,5}, {-10, 10}, {50, 100} }
     */
    public static double[][] calculateBounds(Collection<? extends Bounding> manyBounds) {
        int nDimensions = manyBounds.iterator().next().nDimensions();
        double[][] currentBounds = BoundsUtil.boundToOverride(nDimensions);
        //go through each existing bounds (in each dimension) and find the smallest min and the largest max
        for (Bounding bounds : manyBounds) {
            for (int n = 0; n < nDimensions; n++) {
                double minValN = bounds.getMin(n);
                double maxValN = bounds.getMax(n);
                //current val smaller than nMin
                if (minValN < currentBounds[n][0]) {
                    currentBounds[n][0] = minValN;
                }
                //current val bigger than nMax
                if (maxValN > currentBounds[n][1]) {
                    currentBounds[n][1] = maxValN;
                }
            }
        }
        return currentBounds;
    }


}
