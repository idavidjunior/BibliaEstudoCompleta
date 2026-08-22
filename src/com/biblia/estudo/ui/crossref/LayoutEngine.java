package com.biblia.estudo.ui.crossref;

import android.graphics.Canvas;
import android.view.MotionEvent;

import com.biblia.estudo.model.ReferenceEdge;
import com.biblia.estudo.model.ReferenceNode;

import java.util.List;
import java.util.Map;

/**
 * Interface for layout engines. Organograma is the only visualization mode.
 */
public interface LayoutEngine {
    
    /**
     * Initialize the layout with current data
     */
    void initialize(List<ReferenceNode> nodes, List<ReferenceEdge> edges, Map<Long, ReferenceNode> nodeMap, float viewWidth, float viewHeight);
    
    /**
     * Compute positions for all nodes (called when data changes or layout is switched)
     */
    void computeLayout();
    
    /**
     * Draw all nodes and edges
     */
    void draw(Canvas canvas, float scaleFactor, float translateX, float translateY);
    
    /**
     * Handle touch events - return true if handled
     */
    boolean onTouchEvent(MotionEvent event, float scaleFactor, float translateX, float translateY);
    
    /**
     * Find node at screen coordinates
     */
    ReferenceNode findNodeAt(float screenX, float screenY, float scaleFactor, float translateX, float translateY);
    
    /**
     * Called when a node is dragged
     */
    void onNodeDragged(ReferenceNode node, float dx, float dy);
    
    /**
     * Called when node drag ends (for persistence)
     */
    void onNodeDragEnd(ReferenceNode node);
    
    /**
     * Called when node selection changes
     */
    void onNodeSelected(ReferenceNode node);
    
    /**
     * Center view on a specific node
     */
    void centerOnNode(ReferenceNode node);
    
    /**
     * Fit all nodes to screen
     */
    void fitToScreen();
    
    /**
     * Get the layout type identifier
     */
    LayoutType getType();
    
    /**
     * Get display name for UI
     */
    String getDisplayName();
    
    /**
     * Whether this layout supports node dragging
     */
    boolean supportsDragging();
    
    /**
     * Whether this layout supports expand/collapse
     */
    boolean supportsExpandCollapse();
    
    /**
     * Expand a node (show children)
     */
    void expandNode(ReferenceNode node);
    
    /**
     * Collapse a node (hide children)
     */
    void collapseNode(ReferenceNode node);
    
    /**
     * Clean up resources
     */
    void destroy();
}

/**
 * Single layout type - Organograma
 */
enum LayoutType {
    ORGANOGRAM("Organograma");
    
    private final String displayName;
    
    LayoutType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}