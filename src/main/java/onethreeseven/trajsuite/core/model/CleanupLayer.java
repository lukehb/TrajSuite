package onethreeseven.trajsuite.core.model;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import onethreeseven.trajsuite.core.graphics.GLVboRenderable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * A layer where entities reside for one render loop then they are cleaned up and removed.
 * @author Luke Bermingham
 */
public class CleanupLayer extends AbstractLayer {

    private final Collection<GLVboRenderable> toCleanup = Collections.synchronizedCollection(new ArrayList<>());

    public CleanupLayer(){
        this.setEnabled(true);
        this.setPickEnabled(false);
        this.setName("Trajsuite graphics clean-up layer");
    }

    public void add(GLVboRenderable cleanupGraphic){
        toCleanup.add(cleanupGraphic);
    }

    @Override
    protected void doRender(DrawContext drawContext) {
        //do cleanup, then clear the list
        synchronized (toCleanup){
            for (GLVboRenderable glVboRenderable : toCleanup) {
                glVboRenderable.cleanup(drawContext);
            }
            toCleanup.clear();
        }
    }
}
