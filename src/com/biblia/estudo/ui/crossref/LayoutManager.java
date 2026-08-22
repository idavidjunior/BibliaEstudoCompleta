package com.biblia.estudo.ui.crossref;

import android.content.Context;

import com.biblia.estudo.model.ReferenceEdge;
import com.biblia.estudo.model.ReferenceNode;

import java.util.List;
import java.util.Map;

/**
 * Manages the Organograma layout engine
 */
public class LayoutManager {
    
    private Context context;
    private OrganogramLayoutEngine engine;
    
    private List<ReferenceNode> nodes;
    private List<ReferenceEdge> edges;
    private Map<Long, ReferenceNode> nodeMap;
    private float viewWidth, viewHeight;
    
    // Listeners
    private BaseLayoutEngine.OnNodeClickListener nodeClickListener;
    private BaseLayoutEngine.OnNodeLongClickListener nodeLongClickListener;
    private BaseLayoutEngine.OnMapChangeListener mapChangeListener;
    
    public LayoutManager(Context context) {
        this.context = context;
        this.engine = new OrganogramLayoutEngine(context);
        setListeners();
    }
    
    private void setListeners() {
        engine.setOnNodeClickListener(nodeClickListener);
        engine.setOnNodeLongClickListener(nodeLongClickListener);
        engine.setOnMapChangeListener(mapChangeListener);
    }
    
    public void setData(List<ReferenceNode> nodes, List<ReferenceEdge> edges, Map<Long, ReferenceNode> nodeMap, float viewWidth, float viewHeight) {
        this.nodes = nodes;
        this.edges = edges;
        this.nodeMap = nodeMap;
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        
        engine.initialize(nodes, edges, nodeMap, viewWidth, viewHeight);
        engine.computeLayout();
    }
    
    public void setViewSize(float width, float height) {
        this.viewWidth = width;
        this.viewHeight = height;
        if (nodes != null && edges != null && nodeMap != null) {
            engine.initialize(nodes, edges, nodeMap, width, height);
            engine.computeLayout();
        }
    }
    
    public void resetPositions() {
        if (nodes != null) {
            for (ReferenceNode node : nodes) {
                node.setPositionX(0);
                node.setPositionY(0);
            }
        }
        engine.computeLayout();
    }
    
    public void draw(android.graphics.Canvas canvas, float scaleFactor, float translateX, float translateY) {
        if (engine != null) {
            engine.draw(canvas, scaleFactor, translateX, translateY);
        }
    }
    
    public boolean onTouchEvent(android.view.MotionEvent event, float scaleFactor, float translateX, float translateY) {
        return engine != null && engine.onTouchEvent(event, scaleFactor, translateX, translateY);
    }
    
    public ReferenceNode findNodeAt(float screenX, float screenY, float scaleFactor, float translateX, float translateY) {
        return engine != null ? engine.findNodeAt(screenX, screenY, scaleFactor, translateX, translateY) : null;
    }
    
    public void centerOnNode(ReferenceNode node) {
        if (engine != null) engine.centerOnNode(node);
    }
    
    public void fitToScreen() {
        if (engine != null) engine.fitToScreen();
    }
    
    public void onNodeSelected(ReferenceNode node) {
        if (engine != null) engine.onNodeSelected(node);
    }
    
    public void expandNode(ReferenceNode node) {
        if (engine != null) engine.expandNode(node);
    }
    
    public void collapseNode(ReferenceNode node) {
        if (engine != null) engine.collapseNode(node);
    }
    
    public void onNodeDragEnd(ReferenceNode node) {
        if (engine != null) engine.onNodeDragEnd(node);
    }

    public android.graphics.RectF getContentBounds() {
        if (engine == null) return null;
        return engine.getContentBounds();
    }
    
    public void setOnNodeClickListener(BaseLayoutEngine.OnNodeClickListener listener) {
        this.nodeClickListener = listener;
        engine.setOnNodeClickListener(listener);
    }
    
    public void setOnNodeLongClickListener(BaseLayoutEngine.OnNodeLongClickListener listener) {
        this.nodeLongClickListener = listener;
        engine.setOnNodeLongClickListener(listener);
    }
    
    public void setOnMapChangeListener(BaseLayoutEngine.OnMapChangeListener listener) {
        this.mapChangeListener = listener;
        engine.setOnMapChangeListener(listener);
    }
    
    public void destroy() {
        if (engine != null) {
            engine.destroy();
        }
    }
}