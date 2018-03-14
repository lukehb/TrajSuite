package onethreeseven.trajsuite.core.graphics;

import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.render.DrawContext;
import onethreeseven.trajsuite.core.util.WWExtrasUtil;
import onethreeseven.trajsuitePlugin.graphics.GraphicsPayload;
import onethreeseven.trajsuitePlugin.graphics.PackedVertexData;
import onethreeseven.trajsuitePlugin.graphics.RenderingModes;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 * The base class that things extend if they want to render with a VBO.
 * Doing it this way makes setting up the VBOs far less error prone.
 * @author Luke Bermingham
 */
public class GLVboRenderable extends GLBaseRenderable {

    /**
     * The id of the vertex buffer used to render
     */
    protected Integer vboId = null;

    protected PackedVertexData packedVertexData;

    public GLVboRenderable(BoundingCoordinates model, GraphicsPayload payload, LayerList layerList){
        super(model, payload, layerList);
    }

    @Override
    public void render(DrawContext dc) {
        if (!dc.isPickingMode() && payload.drawOnTop.get()) {
            GL2 gl = dc.getGL().getGL2();
            //clear depth buffer, so we draw on top
            gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
        }
        super.render(dc);
    }

    @Override
    /**
     * Draw the given entity using a tightly packed vertex array, no colors, no normals, no tex coords
     * just vert data.
     */
    public void drawEntity(DrawContext dc) {

        //don't draw in picking mode (this entity is not pickable)
        if (!dc.isPickingMode()) {
            GL2 gl = dc.getGL().getGL2();
            if ((payload.isDirty.get() || vboId == null)) {
                packedVertexData = payload.createVertexData(model);
                vboId = GraphicsUtil.createBuffer(gl, packedVertexData.flushBuffer());
                payload.isDirty.set(false);
            }

            //assume VBO is created now - start drawing
            if (vboId != null && packedVertexData != null) {
                //actual drawing
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId);
                //setup vertex attributes, i.e vertex arrays, color arrays, vertex pointers
                GraphicsUtil.setupVertexAttributes(dc, packedVertexData);
                //set the entity color, this will be ignored if color buffer is being used
                gl.glColor4dv(colorNorm, 0);
                int drawingType = payload.renderingMode.get().mode;
                //draw the trajectory
                gl.glDrawArrays(drawingType, 0, packedVertexData.getNVerts());
                //finish drawing, reset state
                //UNBIND but not delete
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
                //turn off vertex attributes we set up
                GraphicsUtil.disableVertexAttributes(dc, packedVertexData);
            }
        }
    }


    @Override
    protected void beginDrawing(DrawContext dc){
        super.beginDrawing(dc);

        if(!dc.isPickingMode()){
            GL2 gl = dc.getGL().getGL2();
            RenderingModes mode = getPayload().renderingMode.get();

            int preferredSize = getPayload().pointOrLineSize.get();

            //set line width
            if(mode.equals(RenderingModes.LINE_STRIP) || mode.equals(RenderingModes.LINE_LOOP) || mode.equals(RenderingModes.LINES)){

                double pixelSize = getPayload().doScalePointsOrLines.get() ?
                        WWExtrasUtil.scaleToBeDominant(dc, getBoundingBox(), preferredSize) : preferredSize;

                gl.glLineWidth((float) pixelSize);

            }
            //set points size
            else if(mode.equals(RenderingModes.POINTS)){

                gl.glPointSize((float) preferredSize);

                if(payload.smoothPoints.get()){
                    //turn on anti-aliasing (kind of slow though)
                    gl.glEnable(GL2.GL_POINT_SMOOTH);
                }
            }
        }


    }

    @Override
    protected void endDrawing(DrawContext dc){
        super.endDrawing(dc);

        if(!dc.isPickingMode()){
            GL2 gl = dc.getGL().getGL2();

            RenderingModes mode = getPayload().renderingMode.get();

            //set line width
            if(mode.equals(RenderingModes.LINE_STRIP) || mode.equals(RenderingModes.LINE_LOOP) || mode.equals(RenderingModes.LINES)){

                gl.glLineWidth(1);

            }
            //set points size
            else if(mode.equals(RenderingModes.POINTS)){

                gl.glPointSize(1);

                if(payload.smoothPoints.get()){
                    //turn on anti-aliasing (kind of slow though)
                    gl.glDisable(GL2.GL_POINT_SMOOTH);
                }
            }
        }


    }

    @Override
    protected void cleanupRenderable(DrawContext dc) {
        if(vboId != null){
            GraphicsUtil.deleteBuffer(dc.getGL().getGL2(), vboId);
        }
    }

}
