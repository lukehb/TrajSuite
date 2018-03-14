package onethreeseven.trajsuite.core.model;

import gov.nasa.worldwind.layers.LayerList;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import onethreeseven.trajsuite.core.graphics.GLVboRenderable;
import onethreeseven.trajsuitePlugin.graphics.GraphicsPayload;
import onethreeseven.trajsuitePlugin.graphics.GraphicsPrefab;
import onethreeseven.trajsuitePlugin.graphics.RenderingModes;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;
import onethreeseven.trajsuitePlugin.model.Layers;
import onethreeseven.trajsuitePlugin.model.WrappedEntity;
import onethreeseven.trajsuitePlugin.model.WrappedEntityLayer;
import onethreeseven.trajsuitePlugin.transaction.AddEntitiesTransaction;
import java.awt.*;
import java.util.*;

/**
 * The manager of all layers and persistent entities.
 * @author Luke Bermingham
 */
public class TrajsuiteLayers extends Layers {

    private final LayerList renderableLayers;
    private final CleanupLayer cleanupLayer;

    public TrajsuiteLayers(){
        super();
        this.renderableLayers = new LayerList();
        this.cleanupLayer = new CleanupLayer();
        this.renderableLayers.add(cleanupLayer);
    }

    @Override
    protected WrappedEntity newEntity(String entityId, Object model, boolean selected, boolean visible, GraphicsPayload graphicsPayload) {
        if(model instanceof BoundingCoordinates){
            BoundingCoordinates boundingModel = (BoundingCoordinates) model;
            GLVboRenderable renderable = new GLVboRenderable(boundingModel, graphicsPayload, renderableLayers);
            return new RenderableEntity<>(entityId, boundingModel, selected, visible, renderable);
        }
        return super.newEntity(entityId, model, selected, visible, graphicsPayload);
    }

    @Override
    protected WrappedEntityLayer newEntityLayer(String layerName, Map<String, WrappedEntity> entities) {
        Map<String, RenderableEntity> renderableEntitiesmap = new HashMap<>();
        for (Map.Entry<String, WrappedEntity> entry : entities.entrySet()) {
            WrappedEntity entity = entry.getValue();
            if(entity instanceof RenderableEntity){
                renderableEntitiesmap.put(entry.getKey(), (RenderableEntity) entity);
            }
        }

        //the entities map had no renderable entities in it, so do normal wrapped layer
        if(renderableEntitiesmap.isEmpty()){
            return super.newEntityLayer(layerName, entities);
        }

        //add the layer to worldwind
        RenderableLayer layer = new RenderableLayer(layerName, renderableEntitiesmap);
        if(renderableLayers != null){
            renderableLayers.add(layer.getWorldWindLayer());
        }
        return layer;
    }

    @Override
    protected void removeLayerInternal(WrappedEntityLayer layer) {
        super.removeLayerInternal(layer);
        //removes it from worldwind
        if(layer instanceof RenderableLayer){
            renderableLayers.remove(((RenderableLayer) layer).getWorldWindLayer());
        }
    }

    public <T extends BoundingCoordinates> void add(String layername, String id, T model, GraphicsPayload payload){
        AddEntitiesTransaction transaction = new AddEntitiesTransaction();
        transaction.add(layername, id, model, payload);
        process(transaction);
    }

    public LayerList getRenderableLayers() {
        return renderableLayers;
    }

    ////////////////////////////////////////////
    //Properties that count towards edits
    ////////////////////////////////////////////

    final ListChangeListener<GraphicsPrefab> graphicsPrefabsChanged = c -> accumulator.accumulate();
    final ChangeListener<? super Boolean> isDirtyChanged = (ChangeListener<Boolean>) (observable, oldValue, newValue) -> accumulator.accumulate();
    final ChangeListener<? super Color> colorChanged = (ChangeListener<Color>) (observable, oldValue, newValue) -> accumulator.accumulate();
    final ChangeListener<? super Boolean> drawOnTopChanged = (ChangeListener<Boolean>) (observable, oldValue, newValue) -> accumulator.accumulate();
    final ChangeListener<? super Boolean> scalePointsOrLinesChanged = (ChangeListener<Boolean>) (observable, oldValue, newValue) -> accumulator.accumulate();
    final ChangeListener<? super Boolean> smoothPointsChanged = (ChangeListener<Boolean>) (observable, oldValue, newValue) -> accumulator.accumulate();
    final ChangeListener<? super Number> pointOrLineSizeChanged = (ChangeListener<Number>) (observable, oldValue, newValue) -> accumulator.accumulate();
    final ChangeListener<? super RenderingModes> renderingModeChanged = (ChangeListener<RenderingModes>) (observable, oldValue, newValue) -> accumulator.accumulate();

    @Override
    protected void onEntityAdded(WrappedEntity entity) {
        super.onEntityAdded(entity);
        if(entity instanceof RenderableEntity){
            GraphicsPayload payload = ((RenderableEntity) entity).getPayload();
            payload.additionalPrefabs.addListener(graphicsPrefabsChanged);
            payload.isDirty.addListener(isDirtyChanged);
            payload.fallbackColor.addListener(colorChanged);
            payload.drawOnTop.addListener(drawOnTopChanged);
            payload.doScalePointsOrLines.addListener(scalePointsOrLinesChanged);
            payload.smoothPoints.addListener(smoothPointsChanged);
            payload.pointOrLineSize.addListener(pointOrLineSizeChanged);
            payload.renderingMode.addListener(renderingModeChanged);
        }
    }

    @Override
    protected void onEntityRemoved(WrappedEntity entity) {
        super.onEntityRemoved(entity);
        if(entity instanceof RenderableEntity){
            //remove listeners
            GraphicsPayload payload = ((RenderableEntity) entity).getPayload();
            payload.additionalPrefabs.removeListener(graphicsPrefabsChanged);
            payload.isDirty.removeListener(isDirtyChanged);
            payload.fallbackColor.removeListener(colorChanged);
            payload.drawOnTop.removeListener(drawOnTopChanged);
            payload.doScalePointsOrLines.removeListener(scalePointsOrLinesChanged);
            payload.smoothPoints.removeListener(smoothPointsChanged);
            payload.pointOrLineSize.removeListener(pointOrLineSizeChanged);
            payload.renderingMode.removeListener(renderingModeChanged);
            //add the graphic to the cleanup layer
            cleanupLayer.add(((RenderableEntity) entity).renderable);
        }
    }
}
