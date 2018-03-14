package onethreeseven.trajsuite.core.model;

import org.junit.Assert;
import org.junit.Test;
import java.util.Arrays;

/**
 * Tests our bounding objects to make sure it gets the bounds and corners properly.
 * @see Bounding
 * @author Luke Bermingham
 */
public class BoundingTest {

    @Test
    public void testGetCenterArr() throws Exception {
        Bounding bounding = () -> new double[][]{new double[]{0, 5}, new double[]{0, 10}};
        double[] expected = new double[]{2.5, 5};
        double[] actual = bounding.getCenterArr();
        Assert.assertTrue(Arrays.equals(expected, actual));
    }

    @Test
    public void testGetCorners() throws Exception {
        Bounding bounding = () -> new double[][]{new double[]{0, 5}, new double[]{0, 10}};
        double[][] corners = bounding.getCorners();
        double[][] expected = new double[][]{
                new double[]{0, 0},
                new double[]{0, 10},
                new double[]{5, 0},
                new double[]{5, 10}
        };
        System.out.println("Expected corners: ");
        for (double[] corner : expected) {
            System.out.print(Arrays.toString(corner) + " - ");
        }
        System.out.println("\n Got corners: ");
        for (double[] corner : expected) {
            System.out.print(Arrays.toString(corner) + " - ");
        }

        int nCorners = (int) Math.pow(2, bounding.nDimensions());
        for (int i = 0; i < nCorners; i++) {
            Assert.assertTrue(Arrays.equals(expected[i], corners[i]));
        }
    }


}