package com.biblia.estudo.model;

public class Group {
    private long id;
    private String name;
    private int type; // 1 = highlights, 2 = favorites
    private long parentId; // for nested groups (future)
    private int order;
    private long createdAt;
    private int color; // optional color for group

    public static final int TYPE_HIGHLIGHTS = 1;
    public static final int TYPE_FAVORITES = 2;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public long getParentId() { return parentId; }
    public void setParentId(long parentId) { this.parentId = parentId; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}