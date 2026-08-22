package com.biblia.estudo.ui.crossref;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.biblia.estudo.model.ReferenceEdge;
import com.biblia.estudo.model.ReferenceNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReferenceMapView - Main view for cross-references, delegates rendering to Organograma LayoutManager
 */
public class ReferenceMapView extends View {

    private static final float MIN_SCALE = 0.3f;
    private static final float MAX_SCALE = 3.0f;

    private LayoutManager layoutManager;
    private Map<Long, ReferenceNode> nodeMap = new HashMap<>();

    private float scaleFactor = 1.0f;
    private float translateX = 0;
    private float translateY = 0;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private BaseLayoutEngine.OnNodeClickListener nodeClickListener;
    private BaseLayoutEngine.OnNodeLongClickListener nodeLongClickListener;
    private BaseLayoutEngine.OnMapChangeListener mapChangeListener;

    public ReferenceMapView(Context context) {
        this(context, null);
    }

    public ReferenceMapView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReferenceMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        layoutManager = new LayoutManager(context);
        
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        
        // Forward listeners to layout manager
        layoutManager.setOnNodeClickListener(node -> {
            if (nodeClickListener != null) nodeClickListener.onNodeClick(node);
        });
        layoutManager.setOnNodeLongClickListener(node -> {
            return nodeLongClickListener != null && nodeLongClickListener.onNodeLongClick(node);
        });
        layoutManager.setOnMapChangeListener(() -> {
            if (mapChangeListener != null) mapChangeListener.onMapChanged();
        });
    }

    public void setData(List<ReferenceNode> nodes, List<ReferenceEdge> edges) {
        List<ReferenceNode> nodeList = nodes != null ? nodes : new ArrayList<>();
        List<ReferenceEdge> edgeList = edges != null ? edges : new ArrayList<>();
        
        nodeMap.clear();
        for (ReferenceNode node : nodeList) {
            nodeMap.put(node.getId(), node);
        }
        
        // Set data on layout manager with current view size
        layoutManager.setData(nodeList, edgeList, nodeMap, getWidth(), getHeight());
        
        // Initial fit to screen
        layoutManager.fitToScreen();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (layoutManager != null) {
            layoutManager.setViewSize(w, h);
            fitToScreen();
        }
    }

    public void addNode(ReferenceNode node) {
        // This is handled by the activity, not the view directly
        // The view just reflects data from layoutManager
    }

    public void removeNode(long nodeId) {
        // Handled by activity
    }

    public void addEdge(ReferenceEdge edge) {
        // Handled by activity
    }

    public void removeEdge(long edgeId) {
        // Handled by activity
    }

    public void updateNode(ReferenceNode node) {
        nodeMap.put(node.getId(), node);
        invalidate();
    }

    public void updateEdge(ReferenceEdge edge) {
        invalidate();
    }

    public void setOnNodeClickListener(BaseLayoutEngine.OnNodeClickListener listener) {
        this.nodeClickListener = listener;
        layoutManager.setOnNodeClickListener(listener);
    }

    public void setOnNodeLongClickListener(BaseLayoutEngine.OnNodeLongClickListener listener) {
        this.nodeLongClickListener = listener;
        layoutManager.setOnNodeLongClickListener(listener);
    }

    public void setOnMapChangeListener(BaseLayoutEngine.OnMapChangeListener listener) {
        this.mapChangeListener = listener;
        layoutManager.setOnMapChangeListener(listener);
    }

    public void centerOnNode(ReferenceNode node) {
        layoutManager.centerOnNode(node);
        invalidate();
    }

    public void fitToScreen() {
        layoutManager.fitToScreen();
        
        android.graphics.RectF bounds = layoutManager.getContentBounds();
        if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
            float availW = getWidth() - 40f;
            float availH = getHeight() - 40f;
            float scaleX = availW / bounds.width();
            float scaleY = availH / bounds.height();
            scaleFactor = Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.min(scaleX, scaleY)));
            translateX = getWidth() / 2f - (bounds.centerX() * scaleFactor);
            translateY = getHeight() / 2f - (bounds.centerY() * scaleFactor);
        }
        invalidate();
    }

    public void resetPositions() {
        layoutManager.resetPositions();
        layoutManager.fitToScreen();
        invalidate();
    }

    @Override
    protected void onDraw(android.graphics.Canvas canvas) {
        super.onDraw(canvas);
        if (layoutManager != null) {
            layoutManager.draw(canvas, scaleFactor, translateX, translateY);
        }
    }

    private ReferenceNode lastDraggedNode = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        // Forward to layout manager
        boolean handled = layoutManager.onTouchEvent(event, scaleFactor, translateX, translateY);
        
        // Track drag end for persistence
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            lastDraggedNode = layoutManager.findNodeAt(event.getX(), event.getY(), scaleFactor, translateX, translateY);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (lastDraggedNode != null) {
                layoutManager.onNodeDragEnd(lastDraggedNode);
                lastDraggedNode = null;
            }
        }
        
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            float prevScale = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scaleFactor));

            // Adjust translation to keep focus point under finger
            float scaleRatio = scaleFactor / prevScale;
            translateX = focusX - (focusX - translateX) * scaleRatio;
            translateY = focusY - (focusY - translateY) * scaleRatio;

            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            ReferenceNode node = layoutManager.findNodeAt(e.getX(), e.getY(), scaleFactor, translateX, translateY);
            layoutManager.onNodeSelected(node);
            if (node != null) {
                if (nodeClickListener != null) {
                    nodeClickListener.onNodeClick(node);
                }
            }
            invalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            ReferenceNode node = layoutManager.findNodeAt(e.getX(), e.getY(), scaleFactor, translateX, translateY);
            if (node != null && nodeLongClickListener != null) {
                nodeLongClickListener.onNodeLongClick(node);
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // Pan the view
            translateX -= distanceX;
            translateY -= distanceY;
            invalidate();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            ReferenceNode node = layoutManager.findNodeAt(e.getX(), e.getY(), scaleFactor, translateX, translateY);
            if (node != null) {
                centerOnNode(node);
            } else {
                fitToScreen();
            }
            return true;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (layoutManager != null) {
            layoutManager.destroy();
        }
    }
}