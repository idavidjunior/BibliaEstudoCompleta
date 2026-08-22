package com.biblia.estudo.ui.crossref;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import com.biblia.estudo.R;
import com.biblia.estudo.model.ReferenceEdge;
import com.biblia.estudo.model.ReferenceNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Organograma - Layout hierárquico vertical/horizontal
 */
public class OrganogramLayoutEngine extends BaseLayoutEngine {
    
    private boolean horizontal = false;
    private ReferenceNode rootNode = null;
    private Map<Long, List<ReferenceNode>> childrenMap = new HashMap<>();
    private Map<Long, Integer> nodeDepth = new HashMap<>();
    private boolean layoutComputed = false;
    
    private boolean groupedByBook = false;
    private Map<String, List<ReferenceNode>> bookGroups = new LinkedHashMap<>();
    private Map<String, Float> bookGroupCenters = new HashMap<>();
    private Map<String, Integer> bookGroupRow = new HashMap<>();
    private List<Float> rowTopY = new ArrayList<>();
    
    public OrganogramLayoutEngine(Context context) {
        super(context);
    }
    
    public void setHorizontal(boolean horizontal) {
        if (this.horizontal != horizontal) {
            this.horizontal = horizontal;
            layoutComputed = false;
        }
    }
    
    public boolean isHorizontal() {
        return horizontal;
    }
    
    @Override
    protected void onDataChanged() {
        layoutComputed = false;
        rootNode = null; // Force recalculation of root
        buildHierarchy();
    }
    
    private void buildHierarchy() {
        childrenMap.clear();
        nodeDepth.clear();
        bookGroups.clear();
        bookGroupCenters.clear();
        bookGroupRow.clear();
        rowTopY.clear();
        
        // Build adjacency from edges (directed: source -> target)
        Map<Long, List<Long>> adjacency = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();
        
        for (ReferenceNode node : nodes) {
            inDegree.put(node.getId(), 0);
        }
        
        for (ReferenceEdge edge : edges) {
            adjacency.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>()).add(edge.getTargetNodeId());
            inDegree.put(edge.getTargetNodeId(), inDegree.getOrDefault(edge.getTargetNodeId(), 0) + 1);
        }
        
        // Find root (node with in-degree 0, or most connected)
        rootNode = null;
        if (!nodes.isEmpty()) {
            rootNode = nodes.get(0);
            for (ReferenceNode node : nodes) {
                if (inDegree.getOrDefault(node.getId(), 0) == 0) {
                    rootNode = node;
                    break;
                }
            }
        }
        
        // BFS to assign depths and build tree
        if (rootNode != null) {
            assignDepths(rootNode.getId(), 0, adjacency);
            buildTree(rootNode.getId(), adjacency);
            
            // Add disconnected nodes as children of the root so every
            // verse is visible in the organogram.
            for (ReferenceNode node : nodes) {
                if (!nodeDepth.containsKey(node.getId())) {
                    nodeDepth.put(node.getId(), 1);
                    childrenMap.computeIfAbsent(rootNode.getId(), k -> new ArrayList<>()).add(node);
                }
            }
        }
        
        // If there are no connections between verses, group the first
        // level by Bible book so the organogram reads like a meaningful
        // hierarchy (root -> books -> verses) instead of a flat row.
        groupedByBook = adjacency.isEmpty();
        if (groupedByBook) {
            buildBookGroups();
        }
    }
    
    private void buildBookGroups() {
        if (rootNode == null) return;
        
        // Collect depth-1 nodes (direct children of the root)
        List<ReferenceNode> children = new ArrayList<>();
        for (ReferenceNode node : nodes) {
            if (node.getId() != rootNode.getId()) {
                children.add(node);
            }
        }
        
        // Sort by book name, then chapter, then verse
        Collections.sort(children, new Comparator<ReferenceNode>() {
            @Override
            public int compare(ReferenceNode a, ReferenceNode b) {
                int bookCmp = a.getBookName().compareTo(b.getBookName());
                if (bookCmp != 0) return bookCmp;
                int chapterCmp = Integer.compare(a.getChapter(), b.getChapter());
                if (chapterCmp != 0) return chapterCmp;
                return Integer.compare(a.getVerse(), b.getVerse());
            }
        });
        
        for (ReferenceNode node : children) {
            bookGroups.computeIfAbsent(node.getBookName(), k -> new ArrayList<>()).add(node);
        }
    }
    
    private void assignDepths(long nodeId, int depth, Map<Long, List<Long>> adjacency) {
        nodeDepth.put(nodeId, depth);
        List<Long> children = adjacency.getOrDefault(nodeId, new ArrayList<>());
        for (Long childId : children) {
            if (!nodeDepth.containsKey(childId) || nodeDepth.get(childId) > depth + 1) {
                assignDepths(childId, depth + 1, adjacency);
            }
        }
    }
    
    private void buildTree(long nodeId, Map<Long, List<Long>> adjacency) {
        ReferenceNode node = nodeMap.get(nodeId);
        if (node == null) return;
        
        List<Long> children = adjacency.getOrDefault(nodeId, new ArrayList<>());
        for (Long childId : children) {
            ReferenceNode child = nodeMap.get(childId);
            if (child != null && nodeDepth.getOrDefault(childId, 0) == nodeDepth.getOrDefault(nodeId, 0) + 1) {
                childrenMap.computeIfAbsent(nodeId, k -> new ArrayList<>()).add(child);
                buildTree(childId, adjacency);
            }
        }
    }
    
    public void setRootNode(ReferenceNode node) {
        this.rootNode = node;
        layoutComputed = false;
        buildHierarchy();
        computeLayout();
    }
    
    @Override
    public void computeLayout() {
        // Always recompute for layout switching
        layoutComputed = false;
        
        if (nodes.isEmpty()) return;
        
        // Ensure hierarchy is built
        if (rootNode == null) {
            buildHierarchy();
        }
        
        // Group nodes by depth
        Map<Integer, List<ReferenceNode>> depthGroups = new HashMap<>();
        int maxDepth = 0;
        for (Map.Entry<Long, Integer> entry : nodeDepth.entrySet()) {
            ReferenceNode node = nodeMap.get(entry.getKey());
            if (node != null) {
                depthGroups.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(node);
                maxDepth = Math.max(maxDepth, entry.getValue());
            }
        }
        
        // FALLBACK: If no hierarchy (no edges or single node), use simple layout
        if (rootNode == null || depthGroups.isEmpty()) {
            Log.d("OrganogramLayoutEngine", "Fallback to simple layout - nodes: " + nodes.size() + ", edges: " + edges.size());
            float centerX = viewWidth / 2f;
            float centerY = viewHeight / 2f;
            float radius = Math.min(viewWidth, viewHeight) * 0.35f;
            for (int i = 0; i < nodes.size(); i++) {
                ReferenceNode node = nodes.get(i);
                float angle = (i * 2f * (float) Math.PI / Math.max(1, nodes.size()));
                node.setPositionX(centerX + radius * (float) Math.cos(angle));
                node.setPositionY(centerY + radius * (float) Math.sin(angle));
            }
        } else if (groupedByBook && bookGroups.size() > 0) {
            if (horizontal) {
                layoutBookGroupsHorizontal();
            } else {
                layoutBookGroupsVertical();
            }
        } else {
            if (horizontal) {
                layoutHorizontal(depthGroups, maxDepth);
            } else {
                layoutVertical(depthGroups, maxDepth);
            }
        }
        
        layoutComputed = true;
    }
    
    private void layoutBookGroupsVertical() {
        float marginX = 60f;
        float marginY = 80f;
        float boxW = getNodeBoxWidth();
        float boxH = getNodeBoxHeight();
        float availableWidth = viewWidth - 2 * marginX;
        float rowSpacing = boxH + 14f;
        float colGap = 30f;
        float rowGap = 40f;
        
        // Root at top center
        if (rootNode != null) {
            rootNode.setPositionX(viewWidth / 2f);
            rootNode.setPositionY(marginY + 40f);
        }
        
        List<String> bookNames = new ArrayList<>(bookGroups.keySet());
        int groupCount = bookNames.size();
        int colsPerRow = Math.max(1, (int) ((availableWidth + colGap) / (boxW + colGap)));
        int rowCount = (int) Math.ceil((float) groupCount / colsPerRow);
        
        // Max height available per row so everything stays on screen
        float startY = marginY + 40f + boxH / 2f + 90f;
        float maxRowHeight = (viewHeight - startY - marginY - 30f) / Math.max(1, rowCount);
        
        rowTopY.clear();
        bookGroupRow.clear();
        
        float yCursor = startY;
        for (int r = 0; r < rowCount; r++) {
            int from = r * colsPerRow;
            int to = Math.min(groupCount, from + colsPerRow);
            
            // Compute actual height of this row (tallest group)
            float rowH = 0f;
            for (int g = from; g < to; g++) {
                List<ReferenceNode> verses = bookGroups.get(bookNames.get(g));
                if (verses == null || verses.isEmpty()) continue;
                int n = verses.size();
                float rs = rowSpacing;
                if (n > 1 && (n - 1) * rowSpacing + boxH > maxRowHeight) {
                    rs = Math.max(28f, (maxRowHeight - boxH) / (n - 1));
                }
                rowH = Math.max(rowH, (n - 1) * rs + boxH);
            }
            
            rowTopY.add(yCursor);
            
            for (int g = from; g < to; g++) {
                String book = bookNames.get(g);
                List<ReferenceNode> verses = bookGroups.get(book);
                if (verses == null || verses.isEmpty()) continue;
                int n = verses.size();
                float rs = rowSpacing;
                if (n > 1 && (n - 1) * rowSpacing + boxH > maxRowHeight) {
                    rs = Math.max(28f, (maxRowHeight - boxH) / (n - 1));
                }
                float colCenterX = marginX + (g - from) * (boxW + colGap) + boxW / 2f;
                for (int i = 0; i < n; i++) {
                    verses.get(i).setPositionX(colCenterX);
                    verses.get(i).setPositionY(yCursor + boxH / 2f + i * rs);
                }
                bookGroupCenters.put(book, colCenterX);
                bookGroupRow.put(book, r);
            }
            
            yCursor += rowH + rowGap;
        }
    }
    
    private void layoutBookGroupsHorizontal() {
        float marginX = 80f;
        float marginY = 60f;
        float boxW = getNodeBoxWidth();
        float boxH = getNodeBoxHeight();
        float availableHeight = viewHeight - 2 * marginY;
        float verseSpacing = boxW + 14f;
        float rowGap = 30f;
        
        // Root at left center
        if (rootNode != null) {
            rootNode.setPositionX(marginX + 40f);
            rootNode.setPositionY(viewHeight / 2f);
        }
        
        List<String> bookNames = new ArrayList<>(bookGroups.keySet());
        int groupCount = bookNames.size();
        int rowsPerCol = Math.max(1, (int) ((availableHeight + rowGap) / (boxH + rowGap)));
        int colCount = (int) Math.ceil((float) groupCount / rowsPerCol);
        
        float startX = rootNode != null ? rootNode.getPositionX() + boxW / 2f + 90f : marginX + 130f;
        float maxColWidth = (viewWidth - startX - marginX) / Math.max(1, colCount);
        
        rowTopY.clear();
        bookGroupRow.clear();
        
        float xCursor = startX;
        for (int c = 0; c < colCount; c++) {
            int from = c * rowsPerCol;
            int to = Math.min(groupCount, from + rowsPerCol);
            
            // Compute actual width of this column (widest group)
            float colW = 0f;
            for (int g = from; g < to; g++) {
                List<ReferenceNode> verses = bookGroups.get(bookNames.get(g));
                if (verses == null || verses.isEmpty()) continue;
                int n = verses.size();
                float vs = verseSpacing;
                if (n > 1 && (n - 1) * verseSpacing + boxW > maxColWidth) {
                    vs = Math.max(28f, (maxColWidth - boxW) / (n - 1));
                }
                colW = Math.max(colW, (n - 1) * vs + boxW);
            }
            
            rowTopY.add(xCursor);
            
            for (int g = from; g < to; g++) {
                String book = bookNames.get(g);
                List<ReferenceNode> verses = bookGroups.get(book);
                if (verses == null || verses.isEmpty()) continue;
                int n = verses.size();
                float vs = verseSpacing;
                if (n > 1 && (n - 1) * verseSpacing + boxW > maxColWidth) {
                    vs = Math.max(28f, (maxColWidth - boxW) / (n - 1));
                }
                float rowCenterY = marginY + (g - from) * (boxH + rowGap) + boxH / 2f;
                for (int i = 0; i < n; i++) {
                    verses.get(i).setPositionX(xCursor + boxW / 2f + i * vs);
                    verses.get(i).setPositionY(rowCenterY);
                }
                bookGroupCenters.put(book, rowCenterY);
                bookGroupRow.put(book, c);
            }
            
            xCursor += colW + rowGap;
        }
    }
    
    private void layoutVertical(Map<Integer, List<ReferenceNode>> depthGroups, int maxDepth) {
        float marginX = 60f;
        float marginY = 60f;
        float availableWidth = viewWidth - 2 * marginX;
        float availableHeight = viewHeight - 2 * marginY;
        
        float levelHeight = maxDepth > 0 ? availableHeight / (maxDepth + 1) : availableHeight;
        
        for (int depth = 0; depth <= maxDepth; depth++) {
            List<ReferenceNode> levelNodes = depthGroups.get(depth);
            if (levelNodes == null || levelNodes.isEmpty()) continue;
            
            float y = marginY + depth * levelHeight + levelHeight / 2f;
            float spacing = levelNodes.size() > 1 ? availableWidth / (levelNodes.size() - 1) : 0;
            
            for (int i = 0; i < levelNodes.size(); i++) {
                ReferenceNode node = levelNodes.get(i);
                float x = levelNodes.size() == 1 ? viewWidth / 2f : marginX + i * spacing;
                node.setPositionX(x);
                node.setPositionY(y);
            }
        }
    }
    
    private void layoutHorizontal(Map<Integer, List<ReferenceNode>> depthGroups, int maxDepth) {
        float marginX = 60f;
        float marginY = 60f;
        float availableWidth = viewWidth - 2 * marginX;
        float availableHeight = viewHeight - 2 * marginY;
        
        float levelWidth = maxDepth > 0 ? availableWidth / (maxDepth + 1) : availableWidth;
        
        for (int depth = 0; depth <= maxDepth; depth++) {
            List<ReferenceNode> levelNodes = depthGroups.get(depth);
            if (levelNodes == null || levelNodes.isEmpty()) continue;
            
            float x = marginX + depth * levelWidth + levelWidth / 2f;
            float spacing = levelNodes.size() > 1 ? availableHeight / (levelNodes.size() - 1) : 0;
            
            for (int i = 0; i < levelNodes.size(); i++) {
                ReferenceNode node = levelNodes.get(i);
                float y = levelNodes.size() == 1 ? viewHeight / 2f : marginY + i * spacing;
                node.setPositionX(x);
                node.setPositionY(y);
            }
        }
    }
    
    @Override
    public void draw(Canvas canvas, float scaleFactor, float translateX, float translateY) {
        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);
        
        // Draw hierarchy connector lines (organograma classic look)
        if (groupedByBook && bookGroups.size() > 0) {
            drawBookGroupConnectors(canvas);
        } else {
            drawHierarchyConnectors(canvas);
        }
        
        // Draw explicit edges
        for (ReferenceEdge edge : edges) {
            drawEdge(canvas, edge);
        }
        
        // Draw nodes
        for (ReferenceNode node : nodes) {
            if (node.getPositionX() != 0 || node.getPositionY() != 0 || nodes.size() == 1) {
                drawNode(canvas, node);
            }
        }
        
        // Draw book group titles on top
        if (groupedByBook && bookGroups.size() > 0) {
            drawBookGroupTitles(canvas);
        }
        
        canvas.restore();
    }
    
    private void drawBookGroupTitles(Canvas canvas) {
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(resources.getColor(R.color.text_primary));
        titlePaint.setTextSize(16f);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        
        float boxH = getNodeBoxHeight();
        
        for (Map.Entry<String, List<ReferenceNode>> entry : bookGroups.entrySet()) {
            String book = entry.getKey();
            List<ReferenceNode> verses = entry.getValue();
            if (verses == null || verses.isEmpty()) continue;
            
            Float center = bookGroupCenters.get(book);
            if (center == null) continue;
            
            if (horizontal) {
                float x = verses.get(0).getPositionX() - getNodeBoxWidth() / 2f - 10f;
                float y = center;
                canvas.save();
                canvas.rotate(-90f, x, y);
                canvas.drawText(book, x, y, titlePaint);
                canvas.restore();
            } else {
                float x = center;
                float y = verses.get(0).getPositionY() - boxH / 2f - 12f;
                canvas.drawText(book, x, y, titlePaint);
            }
        }
    }
    
    private void drawBookGroupConnectors(Canvas canvas) {
        if (rootNode == null) return;
        
        Paint connectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        connectorPaint.setStyle(Paint.Style.STROKE);
        connectorPaint.setStrokeWidth(2f);
        connectorPaint.setColor(resources.getColor(R.color.divider));
        
        float boxH = getNodeBoxHeight();
        float boxW = getNodeBoxWidth();
        float rootX = rootNode.getPositionX();
        float rootY = rootNode.getPositionY();
        
        // Group books by their row (vertical layout) or column (horizontal)
        Map<Integer, List<String>> rowBooks = new HashMap<>();
        for (String book : bookGroups.keySet()) {
            int row = bookGroupRow.getOrDefault(book, 0);
            rowBooks.computeIfAbsent(row, k -> new ArrayList<>()).add(book);
        }
        
        if (horizontal) {
            // Each column has a vertical bus; a horizontal trunk joins them to the root
            float startX = rootX + boxW / 2f;
            float trunkEndX = startX;
            for (Map.Entry<Integer, List<String>> e : rowBooks.entrySet()) {
                int col = e.getKey();
                if (col >= rowTopY.size()) continue;
                float busX = rowTopY.get(col) - 34f;
                trunkEndX = Math.max(trunkEndX, busX);
            }
            
            Path trunk = new Path();
            trunk.moveTo(startX, rootY);
            trunk.lineTo(trunkEndX, rootY);
            canvas.drawPath(trunk, connectorPaint);
            
            for (Map.Entry<Integer, List<String>> e : rowBooks.entrySet()) {
                int col = e.getKey();
                if (col >= rowTopY.size()) continue;
                float busX = rowTopY.get(col) - 34f;
                
                // Vertical bus spanning the children of this column
                float minChildY = Float.MAX_VALUE;
                float maxChildY = -Float.MAX_VALUE;
                for (String book : e.getValue()) {
                    List<ReferenceNode> verses = bookGroups.get(book);
                    if (verses == null || verses.isEmpty()) continue;
                    for (ReferenceNode v : verses) {
                        minChildY = Math.min(minChildY, v.getPositionY());
                        maxChildY = Math.max(maxChildY, v.getPositionY());
                    }
                }
                if (minChildY <= maxChildY) {
                    Path bus = new Path();
                    bus.moveTo(busX, minChildY);
                    bus.lineTo(busX, maxChildY);
                    canvas.drawPath(bus, connectorPaint);
                }
                
                for (String book : e.getValue()) {
                    List<ReferenceNode> verses = bookGroups.get(book);
                    if (verses == null || verses.isEmpty()) continue;
                    ReferenceNode first = verses.get(0);
                    Path line = new Path();
                    line.moveTo(busX, first.getPositionY());
                    line.lineTo(first.getPositionX() - boxW / 2f, first.getPositionY());
                    canvas.drawPath(line, connectorPaint);
                }
            }
        } else {
            // Each row has a horizontal bus; a vertical trunk joins them to the root
            float startY = rootY + boxH / 2f;
            float trunkEndY = startY;
            for (Map.Entry<Integer, List<String>> e : rowBooks.entrySet()) {
                int row = e.getKey();
                if (row >= rowTopY.size()) continue;
                float busY = rowTopY.get(row) - 34f;
                trunkEndY = Math.max(trunkEndY, busY);
            }
            
            Path trunk = new Path();
            trunk.moveTo(rootX, startY);
            trunk.lineTo(rootX, trunkEndY);
            canvas.drawPath(trunk, connectorPaint);
            
            for (Map.Entry<Integer, List<String>> e : rowBooks.entrySet()) {
                int row = e.getKey();
                if (row >= rowTopY.size()) continue;
                float busY = rowTopY.get(row) - 34f;
                
                // Horizontal bus spanning the children of this row
                float minChildX = Float.MAX_VALUE;
                float maxChildX = -Float.MAX_VALUE;
                for (String book : e.getValue()) {
                    List<ReferenceNode> verses = bookGroups.get(book);
                    if (verses == null || verses.isEmpty()) continue;
                    for (ReferenceNode v : verses) {
                        minChildX = Math.min(minChildX, v.getPositionX());
                        maxChildX = Math.max(maxChildX, v.getPositionX());
                    }
                }
                if (minChildX <= maxChildX) {
                    Path bus = new Path();
                    bus.moveTo(minChildX, busY);
                    bus.lineTo(maxChildX, busY);
                    canvas.drawPath(bus, connectorPaint);
                }
                
                for (String book : e.getValue()) {
                    List<ReferenceNode> verses = bookGroups.get(book);
                    if (verses == null || verses.isEmpty()) continue;
                    ReferenceNode first = verses.get(0);
                    Path line = new Path();
                    line.moveTo(first.getPositionX(), busY);
                    line.lineTo(first.getPositionX(), first.getPositionY() - boxH / 2f);
                    canvas.drawPath(line, connectorPaint);
                }
            }
        }
    }
    
    private void drawHierarchyConnectors(Canvas canvas) {
        Paint connectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        connectorPaint.setStyle(Paint.Style.STROKE);
        connectorPaint.setStrokeWidth(2f);
        connectorPaint.setColor(resources.getColor(R.color.divider));
        
        float boxH = getNodeBoxHeight();
        float boxW = getNodeBoxWidth();
        
        for (Map.Entry<Long, List<ReferenceNode>> entry : childrenMap.entrySet()) {
            ReferenceNode parent = nodeMap.get(entry.getKey());
            if (parent == null) continue;
            float parentX = parent.getPositionX();
            float parentY = parent.getPositionY();
            
            List<ReferenceNode> children = entry.getValue();
            if (children == null || children.isEmpty()) continue;
            
            if (horizontal) {
                // Horizontal: connectors run left to right
                float startX = parentX + boxW / 2f;
                float minChildX = Float.MAX_VALUE;
                float maxChildX = -Float.MAX_VALUE;
                float firstY = Float.MAX_VALUE;
                float lastY = -Float.MAX_VALUE;
                for (ReferenceNode child : children) {
                    if (child == null) continue;
                    minChildX = Math.min(minChildX, child.getPositionX());
                    maxChildX = Math.max(maxChildX, child.getPositionX());
                    firstY = Math.min(firstY, child.getPositionY());
                    lastY = Math.max(lastY, child.getPositionY());
                }
                
                // Vertical bus on the left of the children column
                float busX = (startX + minChildX) / 2f;
                Path bus = new Path();
                bus.moveTo(busX, firstY);
                bus.lineTo(busX, lastY);
                canvas.drawPath(bus, connectorPaint);
                
                // Line from parent to the bus
                Path fromParent = new Path();
                fromParent.moveTo(startX, parentY);
                fromParent.lineTo(busX, parentY);
                canvas.drawPath(fromParent, connectorPaint);
                
                // Lines from bus to each child
                for (ReferenceNode child : children) {
                    if (child == null) continue;
                    Path line = new Path();
                    line.moveTo(busX, child.getPositionY());
                    line.lineTo(child.getPositionX() - boxW / 2f, child.getPositionY());
                    canvas.drawPath(line, connectorPaint);
                }
            } else {
                // Vertical: connectors run top to bottom
                float startY = parentY + boxH / 2f;
                float endY = Float.MAX_VALUE;
                for (ReferenceNode child : children) {
                    if (child == null) continue;
                    endY = Math.min(endY, child.getPositionY() - boxH / 2f);
                }
                float midY = (startY + endY) / 2f;
                
                // Vertical from parent to the bus level
                Path vertical = new Path();
                vertical.moveTo(parentX, startY);
                vertical.lineTo(parentX, midY);
                canvas.drawPath(vertical, connectorPaint);
                
                // Horizontal bus between children
                float minChildX = Float.MAX_VALUE;
                float maxChildX = -Float.MAX_VALUE;
                for (ReferenceNode child : children) {
                    if (child == null) continue;
                    minChildX = Math.min(minChildX, child.getPositionX());
                    maxChildX = Math.max(maxChildX, child.getPositionX());
                }
                if (minChildX <= maxChildX) {
                    Path bus = new Path();
                    bus.moveTo(minChildX, midY);
                    bus.lineTo(maxChildX, midY);
                    canvas.drawPath(bus, connectorPaint);
                }
                
                // Vertical from bus down to each child
                for (ReferenceNode child : children) {
                    if (child == null) continue;
                    Path line = new Path();
                    line.moveTo(child.getPositionX(), midY);
                    line.lineTo(child.getPositionX(), child.getPositionY() - boxH / 2f);
                    canvas.drawPath(line, connectorPaint);
                }
            }
        }
    }
    
    @Override
    public void centerOnNode(ReferenceNode node) {
        // Handled by view
    }
    
    @Override
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
        
        // Include book group titles and connector buses so they aren't clipped
        if (groupedByBook && bookGroups.size() > 0) {
            float extra = 60f;
            if (horizontal) {
                minX = Math.min(minX, rowTopY.isEmpty() ? minX : rowTopY.get(0) - extra);
            } else {
                minY = Math.min(minY, rowTopY.isEmpty() ? minY : rowTopY.get(0) - extra);
            }
        }
        
        return new android.graphics.RectF(minX, minY, maxX, maxY);
    }
    
    @Override
    public LayoutType getType() {
        return LayoutType.ORGANOGRAM;
    }
    
    @Override
    public String getDisplayName() {
        return "Organograma";
    }
    
    @Override
    public boolean supportsDragging() {
        return false;
    }
    
    @Override
    public boolean supportsExpandCollapse() {
        return true;
    }
}