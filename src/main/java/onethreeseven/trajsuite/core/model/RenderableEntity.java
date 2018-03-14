package onethreeseven.trajsuite.core.model;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import onethreeseven.trajsuite.core.graphics.GLVboRenderable;
import onethreeseven.trajsuitePlugin.graphics.GraphicsPayload;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;
import onethreeseven.trajsuitePlugin.model.VisibleEntity;
import onethreeseven.trajsuitePlugin.util.IdGenerator;

/**
 * An entity that implements the methods required to render itself.
 * @param <T> The type of bounding object that we wish to render.
 * @author Luke Bermingham
 */
public class RenderableEntity<T extends BoundingCoordinates> extends VisibleEntity<T> implements Renderable {

    protected final GLVboRenderable renderable;

    protected RenderableEntity(String id, T model, boolean selected, boolean visible, GLVboRenderable renderable){
        super(id, model, selected, visible);
        this.renderable = renderable;

        //update shape/annotation visibility according to entity visibility
        this.isVisibleProperty().addListener(
                (observable, oldValue, newValue) -> {
                    renderable.setPrefabsVisibility(newValue);
                });

        //setup prefab visibility initially
        renderable.setPrefabsVisibility(visible);
    }

    protected RenderableEntity(String id, T model, GLVboRenderable renderable){
        this(id, model, false, true, renderable);
    }

    protected RenderableEntity(T model, GLVboRenderable renderable) {
        this(IdGenerator.nextId(), model, renderable);
    }

    /**
     * Puts the graphic in a state so that when it next draws the internals will be refreshed.
     */
    public GraphicsPayload getPayload(){
        return renderable.getPayload();
    }

    @Override
    public void render(DrawContext dc) {
        if (renderable != null && isVisibleProperty().get()) {
            renderable.render(dc);
        }
    }

}
