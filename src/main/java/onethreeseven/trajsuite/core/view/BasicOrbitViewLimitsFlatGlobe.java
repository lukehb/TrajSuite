package onethreeseven.trajsuite.core.view;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.orbit.BasicOrbitViewLimits;

/**
 * Changing the BasicOrbitViewLimits for FlatGlobe so that 3d viewing is possible.
 * @see BasicOrbitViewLimits
 * @see gov.nasa.worldwind.globes.FlatGlobe
 * @author Luke Bermingham
 */
public class BasicOrbitViewLimitsFlatGlobe extends BasicOrbitViewLimits {

    @Override
    public Angle limitPitch(View view, Angle angle)
    {
        if (view == null)
        {
            String message = Logging.getMessage("nullValue.ViewIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (angle == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return Angle.clamp(angle, this.minPitch, this.maxPitch);
    }

}
