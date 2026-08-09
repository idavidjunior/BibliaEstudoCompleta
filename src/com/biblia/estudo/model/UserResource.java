package com.biblia.estudo.model;

public class UserResource {
    private long id;
    private String title;
    private String uri;
    private String mimeType;
    private long size;
    private long folderId = -1;
    private long parentId = 0; // 0 = raiz da biblioteca; >0 = id de pasta referenciada pai
    private long createdAt;
    private int type; // 0 = REFERENCED_FILE, 1 = REFERENCED_FOLDER, 2 = LOCAL_FOLDER

    public static final int TYPE_REFERENCED_FILE = 0;
    public static final int TYPE_REFERENCED_FOLDER = 1;
    public static final int TYPE_LOCAL_FOLDER = 2;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public long getFolderId() { return folderId; }
    public void setFolderId(long folderId) { this.folderId = folderId; }
    public long getParentId() { return parentId; }
    public void setParentId(long parentId) { this.parentId = parentId; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public String getFileTypeLabel() {
        if (type == TYPE_REFERENCED_FOLDER) return "📂";
        if (type == TYPE_LOCAL_FOLDER) return "📁";
        if (mimeType == null) return "OUTROS";
        if (mimeType.contains("pdf")) return "PDF";
        if (mimeType.contains("msword") || mimeType.contains("officedocument")) return "DOC";
        if (mimeType.contains("spreadsheet") || mimeType.contains("excel")) return "XLS";
        if (mimeType.contains("presentation") || mimeType.contains("powerpoint")) return "PPT";
        if (mimeType.contains("text/")) return "TXT";
        if (mimeType.contains("image/")) return "IMG";
        return "OUTROS";
    }

    public boolean isReferencedFile() { return type == TYPE_REFERENCED_FILE; }
    public boolean isReferencedFolder() { return type == TYPE_REFERENCED_FOLDER; }
    public boolean isLocalFolder() { return type == TYPE_LOCAL_FOLDER; }
}
