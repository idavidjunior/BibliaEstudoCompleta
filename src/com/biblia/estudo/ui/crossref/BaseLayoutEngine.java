package com.biblia.estudo.ui.crossref;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.biblia.estudo.R;
import com.biblia.estudo.model.ReferenceEdge;
import com.biblia.estudo.model.ReferenceNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base abstract class for layout engines with common drawing utilities
 */
public abstract class BaseLayoutEngine implements LayoutEngine {
    
    protected Context context;
    protected Resources resources;
    
    protected List<ReferenceNode> nodes = new ArrayList<>();
    protected List<ReferenceEdge> edges = new ArrayList<>();
    protected Map<Long, ReferenceNode> nodeMap = new HashMap<>();
    
    protected float viewWidth, viewHeight;
    
    // Drawing paints
    protected Paint nodePaint;
    protected Paint nodeBorderPaint;
    protected Paint nodeTextPaint;
    protected Paint edgePaint;
    protected Paint edgeArrowPaint;
    protected Paint selectedNodePaint;
    
    protected ReferenceNode selectedNode = null;
    protected ReferenceNode draggedNode = null;
    
    // Touch handling
    protected float lastTouchX, lastTouchY;
    
    // Listeners
    protected OnNodeClickListener nodeClickListener;
    protected OnNodeLongClickListener nodeLongClickListener;
    protected OnMapChangeListener mapChangeListener;
    
    public interface OnNodeClickListener {
        void onNodeClick(ReferenceNode node);
    }
    
    public interface OnNodeLongClickListener {
        boolean onNodeLongClick(ReferenceNode node);
    }
    
    public interface OnMapChangeListener {
        void onMapChanged();
    }
    
    public BaseLayoutEngine(Context context) {
        this.context = context;
        this.resources = context.getResources();
        initPaints();
    }
    
    protected void initPaints() {
        nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodePaint.setStyle(Paint.Style.FILL);
        
        nodeBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodeBorderPaint.setStyle(Paint.Style.STROKE);
        nodeBorderPaint.setStrokeWidth(3f);
        
        nodeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodeTextPaint.setTextSize(12f);
        nodeTextPaint.setTextAlign(Paint.Align.CENTER);
        nodeTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        
        edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(2f);
        
        edgeArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgeArrowPaint.setStyle(Paint.Style.FILL);
        
        selectedNodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedNodePaint.setStyle(Paint.Style.STROKE);
        selectedNodePaint.setStrokeWidth(4f);
    }
    
    @Override
    public void initialize(List<ReferenceNode> nodes, List<ReferenceEdge> edges, Map<Long, ReferenceNode> nodeMap, float viewWidth, float viewHeight) {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
        this.edges = edges != null ? edges : new ArrayList<>();
        this.nodeMap = nodeMap != null ? nodeMap : new HashMap<>();
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        
        // Ensure nodeMap is populated
        this.nodeMap.clear();
        for (ReferenceNode node : this.nodes) {
            this.nodeMap.put(node.getId(), node);
        }
        
        onDataChanged();
    }
    
    protected void onDataChanged() {
        // Override in subclasses
    }
    
    public void forceComputeLayout() {
        computeLayout();
    }
    
    @Override
    public void draw(Canvas canvas, float scaleFactor, float translateX, float translateY) {
        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);
        
        // Draw edges first
        for (ReferenceEdge edge : edges) {
            drawEdge(canvas, edge);
        }
        
        // Draw nodes
        for (ReferenceNode node : nodes) {
            if (node.getPositionX() != 0 || node.getPositionY() != 0 || nodes.size() == 1) {
                drawNode(canvas, node);
            }
        }
        
        canvas.restore();
    }
    
    protected void drawNode(Canvas canvas, ReferenceNode node) {
        float x = node.getPositionX();
        float y = node.getPositionY();
        float boxW = getNodeBoxWidth();
        float boxH = getNodeBoxHeight();
        float left = x - boxW / 2f;
        float top = y - boxH / 2f;
        float right = x + boxW / 2f;
        float bottom = y + boxH / 2f;
        
        // Set node fill color
        if (node.getColor() != null) {
            try {
                nodePaint.setColor(Color.parseColor(node.getColor()));
            } catch (Exception e) {
                nodePaint.setColor(resources.getColor(R.color.surface));
            }
        } else {
            nodePaint.setColor(resources.getColor(R.color.surface));
        }
        RectF boxRect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(boxRect, 8f, 8f, nodePaint);
        
        // Draw border
        if (node == selectedNode) {
            nodeBorderPaint.setColor(resources.getColor(R.color.accent));
            nodeBorderPaint.setStrokeWidth(4f);
        } else {
            nodeBorderPaint.setColor(resources.getColor(R.color.accent));
            nodeBorderPaint.setStrokeWidth(2f);
        }
        canvas.drawRoundRect(boxRect, 8f, 8f, nodeBorderPaint);
        
        // Node reference text (inside box, top)
        String ref = node.getReference();
        nodeTextPaint.setTextSize(12f);
        nodeTextPaint.setTextAlign(Paint.Align.CENTER);
        nodeTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        setTextColorFor(node);
        float textY = top + 20f;
        canvas.drawText(ref, x, textY, nodeTextPaint);
        
        // Verse text (inside box, bottom, truncated)
        String verse = node.getVerseText();
        if (verse == null || verse.trim().isEmpty()) {
            verse = "";
        }
        nodeTextPaint.setTextSize(9f);
        nodeTextPaint.setTypeface(android.graphics.Typeface.DEFAULT);
        String truncated = truncateToWidth(verse, boxW - 16f);
        if (!truncated.isEmpty()) {
            float verseY = top + boxH - 12f;
            canvas.drawText(truncated, x, verseY, nodeTextPaint);
        }
        
        // Connection badge
        if (node.getConnectionCount() > 0) {
            drawConnectionBadge(canvas, node, boxW / 2f);
        }
    }
    
    protected float getNodeBoxWidth() {
        return 150f;
    }
    
    protected float getNodeBoxHeight() {
        return 52f;
    }
    
    protected void setTextColorFor(ReferenceNode node) {
        if (node.getColor() != null) {
            try {
                int bgColor = Color.parseColor(node.getColor());
                double luminance = (0.299 * Color.red(bgColor) + 0.587 * Color.green(bgColor) + 0.114 * Color.blue(bgColor)) / 255;
                nodeTextPaint.setColor(luminance > 0.5 ? resources.getColor(R.color.text_primary) : resources.getColor(R.color.text_light));
            } catch (Exception e) {
                nodeTextPaint.setColor(resources.getColor(R.color.text_primary));
            }
        } else {
            nodeTextPaint.setColor(resources.getColor(R.color.text_primary));
        }
    }
    
    protected String truncateToWidth(String text, float maxWidth) {
        if (text == null || text.isEmpty()) return "";
        if (nodeTextPaint.measureText(text) <= maxWidth) return text;
        String result = text;
        while (result.length() > 1 && nodeTextPaint.measureText(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }
    
    protected void drawConnectionBadge(Canvas canvas, ReferenceNode node, float radius) {
        Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgePaint.setColor(resources.getColor(R.color.accent));
        badgePaint.setStyle(Paint.Style.FILL);
        
        Paint badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeTextPaint.setColor(resources.getColor(R.color.text_light));
        badgeTextPaint.setTextSize(10f);
        badgeTextPaint.setTextAlign(Paint.Align.CENTER);
        badgeTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        
        float badgeRadius = 12f;
        float badgeX = node.getPositionX() + radius - 8;
        float badgeY = node.getPositionY() - radius + 8;
        
        canvas.drawCircle(badgeX, badgeY, badgeRadius, badgePaint);
        canvas.drawText(String.valueOf(node.getConnectionCount()), badgeX, badgeY + 3.5f, badgeTextPaint);
    }
    
    protected void drawEdge(Canvas canvas, ReferenceEdge edge) {
        ReferenceNode source = nodeMap.get(edge.getSourceNodeId());
        ReferenceNode target = nodeMap.get(edge.getTargetNodeId());
        
        if (source == null || target == null) return;
        
        float sx = source.getPositionX();
        float sy = source.getPositionY();
        float tx = target.getPositionX();
        float ty = target.getPositionY();
        
        float dx = tx - sx;
        float dy = ty - sy;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance == 0) return;
        
        float ux = dx / distance;
        float uy = dy / distance;
        
        PointF start = getBoxBoundaryPoint(source, dx, dy);
        PointF end = getBoxBoundaryPoint(target, -dx, -dy);
        float startX = start.x;
        float startY = start.y;
        float endX = end.x;
        float endY = end.y;
        
        // Draw curved line
        drawEdgePath(canvas, startX, startY, endX, endY, ux, uy);
        
        // Arrow head
        drawArrowHead(canvas, endX, endY, ux, uy);
    }
    
    protected void drawEdgePath(Canvas canvas, float startX, float startY, float endX, float endY, float ux, float uy) {
        float midX = (startX + endX) / 2f;
        float midY = (startY + endY) / 2f;
        float perpX = -uy * 30f;
        float perpY = ux * 30f;
        
        Path path = new Path();
        path.moveTo(startX, startY);
        path.quadTo(midX + perpX, midY + perpY, endX, endY);
        canvas.drawPath(path, edgePaint);
    }
    
    protected PointF getBoxBoundaryPoint(ReferenceNode node, float dx, float dy) {
        float hw = getNodeBoxWidth() / 2f;
        float hh = getNodeBoxHeight() / 2f;
        float tx = Math.abs(dx) > 0f ? hw / Math.abs(dx) : Float.MAX_VALUE;
        float ty = Math.abs(dy) > 0f ? hh / Math.abs(dy) : Float.MAX_VALUE;
        float t = Math.min(tx, ty);
        return new PointF(node.getPositionX() + dx * t, node.getPositionY() + dy * t);
    }
    
    protected void drawArrowHead(Canvas canvas, float x, float y, float ux, float uy) {
        float arrowSize = 12f;
        float angle = (float) Math.PI / 6f;
        
        float baseAngle = (float) Math.atan2(uy, ux);
        float ax1 = x - arrowSize * (float) Math.cos(baseAngle - angle);
        float ay1 = y - arrowSize * (float) Math.sin(baseAngle - angle);
        float ax2 = x - arrowSize * (float) Math.cos(baseAngle + angle);
        float ay2 = y - arrowSize * (float) Math.sin(baseAngle + angle);
        
        Path arrowPath = new Path();
        arrowPath.moveTo(x, y);
        arrowPath.lineTo(ax1, ay1);
        arrowPath.lineTo(ax2, ay2);
        arrowPath.close();
        canvas.drawPath(arrowPath, edgeArrowPaint);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event, float scaleFactor, float translateX, float translateY) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                draggedNode = findNodeAt(event.getX(), event.getY(), scaleFactor, translateX, translateY);
                if (draggedNode != null && supportsDragging()) {
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (draggedNode != null && event.getPointerCount() == 1 && supportsDragging()) {
                    float dx = (event.getX() - lastTouchX) / scaleFactor;
                    float dy = (event.getY() - lastTouchY) / scaleFactor;
                    onNodeDragged(draggedNode, dx, dy);
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    if (mapChangeListener != null) mapChangeListener.onMapChanged();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (draggedNode != null) {
                    draggedNode = null;
                    return true;
                }
                break;
        }
        return false;
    }
    
    @Override
    public ReferenceNode findNodeAt(float screenX, float screenY, float scaleFactor, float translateX, float translateY) {
        float worldX = (screenX - translateX) / scaleFactor;
        float worldY = (screenY - translateY) / scaleFactor;
        float hw = getNodeBoxWidth() / 2f;
        float hh = getNodeBoxHeight() / 2f;
        
        for (ReferenceNode node : nodes) {
            float dx = Math.abs(worldX - node.getPositionX());
            float dy = Math.abs(worldY - node.getPositionY());
            if (dx <= hw && dy <= hh) {
                return node;
            }
        }
        return null;
    }
    
    @Override
    public void onNodeDragged(ReferenceNode node, float dx, float dy) {
        node.setPositionX(node.getPositionX() + dx);
        node.setPositionY(node.getPositionY() + dy);
    }
    
    @Override
    public void onNodeDragEnd(ReferenceNode node) {
        // Default: nothing, override in subclasses if needed
    }
    
    @Override
    public void onNodeSelected(ReferenceNode node) {
        selectedNode = node;
    }
    
    @Override
    public void centerOnNode(ReferenceNode node) {
        // Override in subclasses if needed
    }
    
    @Override
    public void fitToScreen() {
        if (nodes.isEmpty()) return;
        
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float radius = 48f;
        
        for (ReferenceNode node : nodes) {
            float x = node.getPositionX();
            float y = node.getPositionY();
            minX = Math.min(minX, x - radius);
            maxX = Math.max(maxX, x + radius);
            minY = Math.min(minY, y - radius);
            maxY = Math.max(maxY, y + radius);
        }
        
        float contentWidth = maxX - minX;
        float contentHeight = maxY - minY;
        
        if (contentWidth == 0 || contentHeight == 0) return;
        
        float scaleX = viewWidth / contentWidth * 0.8f;
        float scaleY = viewHeight / contentHeight * 0.8f;
        // Store fit-to-screen parameters for the view to use
    }

    public android.graphics.RectF getContentBounds() {
        if (nodes.isEmpty()) return null;
        
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float hw = getNodeBoxWidth() / 2f;
        float hh = getNodeBoxHeight() / 2f;
        
        for (ReferenceNode node : nodes) {
            float x = node.getPositionX();
            float y = node.getPositionY();
            minX = Math.min(minX, x - hw);
            maxX = Math.max(maxX, x + hw);
            minY = Math.min(minY, y - hh);
            maxY = Math.max(maxY, y + hh);
        }
        return new android.graphics.RectF(minX, minY, maxX, maxY);
    }
    
    @Override
    public void expandNode(ReferenceNode node) {
        node.setExpanded(true);
        if (mapChangeListener != null) mapChangeListener.onMapChanged();
    }
    
    @Override
    public void collapseNode(ReferenceNode node) {
        node.setExpanded(false);
        if (mapChangeListener != null) mapChangeListener.onMapChanged();
    }
    
    @Override
    public void destroy() {
        // Clean up
    }
    
    // Setters for listeners
    public void setOnNodeClickListener(OnNodeClickListener listener) {
        this.nodeClickListener = listener;
    }
    
    public void setOnNodeLongClickListener(OnNodeLongClickListener listener) {
        this.nodeLongClickListener = listener;
    }
    
    public void setOnMapChangeListener(OnMapChangeListener listener) {
        this.mapChangeListener = listener;
    }
    
    public void updateNode(ReferenceNode node) {
        // Default: update nodeMap
        nodeMap.put(node.getId(), node);
    }
    
    public void updateEdge(ReferenceEdge edge) {
        // Default: nothing, edges don't have mutable visual state
    }
}