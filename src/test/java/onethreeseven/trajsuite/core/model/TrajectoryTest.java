package onethreeseven.trajsuite.core.model;


import onethreeseven.datastructures.model.Trajectory;
import org.junit.Assert;
import org.junit.Test;
import java.util.Arrays;

/**
 * Test a trajectory and its supported methods.
 * @see Trajectory
 * @author Luke Bermingham
 */
public class TrajectoryTest {

    @Test
    public void testIterator() throws Exception {
        int nPts = 10;
        double[][] pts = new double[nPts][2];
        for (int i = 0; i < nPts; i++) {
            pts[i][0] = i;
            pts[i][1] = 0;
        }

        Trajectory trajectory = new Trajectory(pts);

        int counter = 0;
        for (double[] pt : trajectory) {
            Assert.assertTrue(Arrays.equals(pts[counter], pt));
            counter++;
        }
        Assert.assertTrue(counter == nPts);

    }
}