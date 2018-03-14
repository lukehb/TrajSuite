package onethreeseven.trajsuite.core.graphics;

import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.OGLUtil;
import javafx.collections.ListChangeListener;
import onethreeseven.trajsuite.core.model.Bounding;
import onethreeseven.trajsuitePlugin.graphics.GraphicsPayload;
import onethreeseven.trajsuitePlugin.graphics.GraphicsPrefab;
import onethreeseven.trajsuitePlugin.graphics.LabelPrefab;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * All graphics objects that want to draw to screen in this project
 * must in some way have this as their parent.
 * It provides helpful functionality to all classes that require rendering.
 * @author Luke Bermingham
 */
public abstract class GLBaseRenderable implements Renderable {

    protected final BoundingCoordinates model;

    protected final GraphicsPayload payload;
    protected final double[] colorNorm = new double[]{1.0, 0, 0, 1.0};

    private Box boxBounds = null;

    //Annotation
    protected Collection<AbstractAnnotation> annotations;
    private final AnnotationLayer annotationLayer;

    //shapes
    private final gov.nasa.worldwind.layers.RenderableLayer shapesLayer;
    protected final Collection<AbstractSurfaceShape> surfaceShapes;

    public GLBaseRenderable(BoundingCoordinates model, GraphicsPayload payload, LayerList layerList){
        this.model = model;
        this.payload = payload;

        //always dirty at first, so we make the packed vertex data
        this.payload.isDirty.setValue(true);

        this.payload.fallbackColor.addListener((observable, oldValue, newValue) -> {
            updateColorNorm(newValue);
        });

        //set color initially
        updateColorNorm(this.payload.fallbackColor.get());

        //////////////////////////
        //SETUP annotation stuff
        //////////////////////////

        //ensure we have an annotation layer in case one of graphics has an annotation
        List<Layer> res = layerList.getLayersByClass(AnnotationLayer.class);
        if(!res.isEmpty()){
            annotationLayer = (AnnotationLayer) res.iterator().next();
            annotationLayer.setEnabled(true);
        }else{
            annotationLayer = new AnnotationLayer();
            annotationLayer.setEnabled(true);
            annotationLayer.setName("Annotations");
            layerList.add(annotationLayer);
        }

        //ensure we have a shapes layers
        Layer layer = layerList.getLayerByName("Shapes");
        if(layer == null || !(layer instanceof gov.nasa.worldwind.layers.RenderableLayer)){
            layer = new gov.nasa.worldwind.layers.RenderableLayer();
            layer.setName("Shapes");
            layer.setEnabled(true);

            shapesLayer = (gov.nasa.worldwind.layers.RenderableLayer) layer;
            layerList.add(shapesLayer);
        }else{
            shapesLayer = (gov.nasa.worldwind.layers.RenderableLayer) layer;
            shapesLayer.setEnabled(true);
        }

        ////////////////////
        //Setup prefab stuff
        ////////////////////
        this.surfaceShapes = new ArrayList<>();
        this.annotations = new ArrayList<>();

        updatePrefabGraphics();

        //add change listener so when prefabs shapes change so do our surface shapes
        this.payload.additionalPrefabs.addListener(
                (ListChangeListener<GraphicsPrefab>) c -> updatePrefabGraphics());

    }

    protected void updateColorNorm(Color color){
        colorNorm[0] = color.getRed() / 255d;
        colorNorm[1] = color.getGreen() / 255d;
        colorNorm[2] = color.getBlue() / 255d;
        colorNorm[3] = color.getAlpha() / 255d;
    }

    public void setPrefabsVisibility(boolean visible){
        if(!surfaceShapes.isEmpty()){
            for (AbstractSurfaceShape surfaceShape : surfaceShapes) {
                surfaceShape.setVisible(visible);
            }
        }
        if(!annotations.isEmpty()){
            for (AbstractAnnotation annotation : annotations) {
                annotation.getAttributes().setVisible(visible);
            }
        }
    }

    protected void updatePrefabGraphics(){
        //remove shapes from layer
        for (AbstractSurfaceShape surfaceShape : surfaceShapes) {
            shapesLayer.removeRenderable(surfaceShape);
        }
        //remove annotations from layer
        for (AbstractAnnotation annotation : annotations) {
            annotationLayer.removeAnnotation(annotation);
        }

        //clear the references to them too
        surfaceShapes.clear();
        annotations.clear();

        for (GraphicsPrefab additionalPrefab : getPayload().additionalPrefabs) {
            //handle adding label
            if(additionalPrefab instanceof LabelPrefab){
                LabelPrefab labelPrefab = (LabelPrefab) additionalPrefab;
                if(!labelPrefab.label.get().isEmpty()){
                    if(labelPrefab.isAnnotation){
                        AbstractAnnotation annotation = PrefabToRenderableFactory.createAnnotation(labelPrefab);
                        annotations.add(annotation);
                        annotationLayer.addAnnotation(annotation);
                    }
                    else{
                        SurfaceText surfaceText = PrefabToRenderableFactory.createText(labelPrefab);
                        shapesLayer.addRenderable(surfaceText);
                    }

                }
            }
            //handle adding shape
            else{
                AbstractSurfaceShape shape = PrefabToRenderableFactory.createAnnotation(additionalPrefab);
                surfaceShapes.add(shape);
                shapesLayer.addRenderable(shape);
            }
        }
    }

    /**
     * Determines whether the renderable intersects the view frustum.
     *
     * @param dc the current draw context.
     * @param extent the extent to test intersection against
     * @return true if this object intersects the frustum, otherwise false.
     */
    protected boolean intersectsFrustum(DrawContext dc, Extent extent) {
        if (extent == null)
            return true; // don't know the visibility, shape hasn't been computed yet

        if (dc.isPickingMode())
            return dc.getPickFrustums().intersectsAny(extent);

        return dc.getView().getFrustumInModelCoordinates().intersects(extent);
    }


    /**
     * Setup drawing state. State changed by this method must be restored in
     * endDrawing.
     *
     * @param dc Active draw context.
     */
    protected void beginDrawing(DrawContext dc) {

        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

        int attrMask = GL2.GL_CURRENT_BIT | GL2.GL_COLOR_BUFFER_BIT;

        gl.glPushAttrib(attrMask);

        //enable color
        gl.glEnable(GL2.GL_COLOR_MATERIAL); //enable color

        if (!dc.isPickingMode()) {
            dc.beginStandardLighting();
            gl.glEnable(GL.GL_BLEND);
            OGLUtil.applyBlending(gl, false);

            //We're applying a scale transform on the modelview matrix, so it
            // must be re-normalized before lighting is computed.
            gl.glEnable(GL2.GL_NORMALIZE);
        }

    }

    @Override
    public void render(DrawContext dc) {
        // Render is called three times:
        // 1) During picking. The cube is drawn in a single color.
        // 2) As a normal renderable. The cube is added to the ordered renderable queue.
        // 3) As an OrderedRenderable. The cube is drawn.


        //comment this block out to disable frustum culling
        Extent extent = getBoundingBox();
        if (extent != null) {
            if (!this.intersectsFrustum(dc, extent))
                return;

            // If the shape is less that a pixel in size, don't render it.
            if (dc.isSmall(extent, 1))
                return;
        }
        //end block

        if (dc.getViewportCenterPosition() == null) {
            return;
        } //dont render at all if the earth is no under the viewport

        this.drawRenderable(dc);
    }

    /**
     * Set up drawing state, and draw. This method is called when something is rendered in ordered rendering
     * mode.
     *
     * @param dc Current draw context.
     */
    protected void drawRenderable(DrawContext dc) {
        this.beginDrawing(dc);
        try {
            drawEntity(dc);
        } finally {
            this.endDrawing(dc);
        }
    }

    /**
     * Restore drawing state changed in beginDrawing to the default.
     *
     * @param dc Active draw context.
     */
    protected void endDrawing(DrawContext dc) {
        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

        if (!dc.isPickingMode()) {
            dc.endStandardLighting();
        }


        gl.glDisable(GL2.GL_COLOR_MATERIAL); //disable color
        gl.glPopAttrib();
    }

    public void cleanup(DrawContext dc) {
        for (AbstractAnnotation annotation : annotations) {
            annotationLayer.removeAnnotation(annotation);
        }
        for (AbstractSurfaceShape surfaceShape : surfaceShapes) {
            shapesLayer.removeRenderable(surfaceShape);
        }
        annotations.clear();
        surfaceShapes.clear();
        cleanupRenderable(dc);
    }

    /**
     * @return The bounding box of the model (in world coordinates).
     * Once this is called once it is cached (so the box is not recalculated).
     */
    public Box getBoundingBox(){
        if(boxBounds == null){
            Bounding bounding = model::getBounds;
            boxBounds = bounding.getBox();
        }
        return boxBounds;
    }

    public GraphicsPayload getPayload() {
        return payload;
    }

    //ABSTRACT Methods

    /**
     * The actual rendering code for the object
     *
     * @param dc The drawing context of worldwind
     */
    public abstract void drawEntity(DrawContext dc);

    /**
     * Called when the renderable is being destroyed
     * @param dc draw context
     */
    protected abstract void cleanupRenderable(DrawContext dc);

}
