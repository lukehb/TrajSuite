package onethreeseven.trajsuite.core.view;

import gov.nasa.worldwind.awt.AWTInputHandler;
import java.awt.event.MouseEvent;

/**
 * Some changes to the AWTInputHandler
 * 1) It doesn't redraw on mouse move.
 * @see AWTInputHandler for the original
 * @author Luke Bermingham
 */
public class CheapAWTInputHandler extends AWTInputHandler {

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        if (this.wwd == null)
        {
            return;
        }

        if (mouseEvent == null)
        {
            return;
        }

        this.mousePoint = mouseEvent.getPoint();
        this.callMouseMovedListeners(mouseEvent);

        if (!mouseEvent.isConsumed())
        {
            this.wwd.getView().getViewInputHandler().mouseMoved(mouseEvent);
        }

        // Redraw to update the current position and selection.
        if (this.wwd.getSceneController() != null)
        {
            this.wwd.getSceneController().setPickPoint(mouseEvent.getPoint());

            //DON'T REDRAW ON MOUSE MOVE!

            //this.wwd.redraw();
        }
    }
}
