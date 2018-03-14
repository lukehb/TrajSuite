package onethreeseven.trajsuite.core.model;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.Logging;
import onethreeseven.trajsuite.core.graphics.GLVboRenderable;
import onethreeseven.trajsuitePlugin.model.VisibleEntityLayer;
import onethreeseven.trajsuitePlugin.model.WrappedEntity;

import javax.media.opengl.GL2;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * A layer of {@link Renderable} that are added to the layer list for rendering.
 */
public class RenderableLayer extends VisibleEntityLayer {

    private final AbstractLayer wwLayer;

    RenderableLayer(String layername, Map<String, RenderableEntity> entities){
        super(layername, entities, true);
        this.wwLayer = makeWWLayer();
    }

    RenderableLayer(String layerName) {
        super(layerName, true);
        this.wwLayer = makeWWLayer();
    }

    public void add(RenderableEntity entity){
        super.add(entity);
    }

    public RenderableEntity remove(String entityId){
        WrappedEntity entity = super.remove(entityId);
        return (RenderableEntity) entity;
    }

    @Override
    public RenderableEntity get(String id) {
        return (RenderableEntity) super.get(id);
    }

    public AbstractLayer getWorldWindLayer() {
        return wwLayer;
    }

    private AbstractLayer makeWWLayer(){
        AbstractLayer wwLayer = new AbstractLayer() {

            private final PickSupport pickSupport = new PickSupport();

            @Override
            protected void doRender(DrawContext dc) {
                for (Object entity : RenderableLayer.this) {
                    if(entity instanceof RenderableEntity){
                        if(((RenderableEntity) entity).isVisibleProperty().get()){
                            ((RenderableEntity) entity).render(dc);
                        }
                    }
                }
            }

            @Override
            public String getName() {
                return RenderableLayer.this.getLayerName();
            }

            //Picking
            @Override
            protected void doPick(DrawContext dc, Point pickPoint) {
                //only do picking if the layer is visible
                if (isEnabled()) {
                    GL2 gl = dc.getGL().getGL2();
                    //enable the color bit, essential for picking
                    gl.glEnable(GL2.GL_COLOR_MATERIAL);
                    pickSupport.clearPickList();
                    pickSupport.beginPicking(dc);

                    try {
                        for (Object entityObj : RenderableLayer.this) {
                            if(entityObj instanceof RenderableEntity){
                                RenderableEntity entity = (RenderableEntity) entityObj;
                                //set the unique picking color
                                java.awt.Color color = dc.getUniquePickColor();
                                gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
                                try {
                                    entity.render(dc);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    String msg = Logging.getMessage("generic.ExceptionWhilePickingRenderable");
                                    Logging.logger().severe(msg);
                                    Logging.logger().log(java.util.logging.Level.FINER, msg, e); // show exception for this level
                                    continue; // go on to next renderable
                                }

                                //add a pickable object
                                pickSupport.addPickableObject(color.getRGB(), entity);
                            }

                        }

                        pickSupport.resolvePick(dc, pickPoint, this);
                    } finally {
                        pickSupport.endPicking(dc);
                        //disable color bit
                        gl.glDisable(GL2.GL_COLOR_MATERIAL);
                    }
                }
            }

        };

        //by default turn picking off
        wwLayer.setPickEnabled(false);
        return wwLayer;
    }

}
