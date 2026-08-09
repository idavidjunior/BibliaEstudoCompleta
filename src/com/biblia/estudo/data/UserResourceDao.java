package com.biblia.estudo.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import com.biblia.estudo.model.UserResource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserResourceDao {

    private static final String TABLE_NAME = "user_resources";
    private static final int MAX_IMPORT_DEPTH = 12;
    private SQLiteDatabase db;

    public static final int SORT_NAME = 0;
    public static final int SORT_TYPE = 1;
    public static final int SORT_SIZE = 2;
    public static final int SORT_DATE = 3;

    public UserResourceDao(SQLiteDatabase db) {
        this.db = db;
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "uri TEXT NOT NULL," +
                "mime_type TEXT," +
                "file_size INTEGER DEFAULT 0," +
                "folder_id INTEGER DEFAULT -1," +
                "created_at INTEGER NOT NULL," +
                "type INTEGER DEFAULT 0" +
                ")");
        try { db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN folder_id INTEGER DEFAULT -1"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN type INTEGER DEFAULT 0"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN parent_id INTEGER DEFAULT 0"); } catch (Exception ignored) {}
        try { db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_uri_type ON " + TABLE_NAME + " (uri, type)"); } catch (Exception ignored) {}
    }

    public long insert(UserResource res) {
        ContentValues cv = new ContentValues();
        cv.put("title", res.getTitle());
        cv.put("uri", res.getUri());
        cv.put("mime_type", res.getMimeType());
        cv.put("file_size", res.getSize());
        cv.put("folder_id", res.getFolderId());
        cv.put("parent_id", res.getParentId());
        cv.put("created_at", res.getCreatedAt());
        cv.put("type", res.getType());
        return db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<UserResource> getAll() {
        return getByFolderAndType(-2, -1);
    }

    public List<UserResource> getByFolder(long folderId) {
        return getByFolderAndType(folderId, -1);
    }

    public List<UserResource> getByFolder(long folderId, int sort) {
        List<UserResource> list = new ArrayList<>();
        String selection = folderId == -1 ? "(folder_id IS NULL OR folder_id=-1)" : "folder_id=?";
        String[] args = folderId >= 0 ? new String[]{String.valueOf(folderId)} : null;
        Cursor c = db.query(TABLE_NAME, null, selection, args, null, null, orderClause(sort, true));
        if (c != null) {
            while (c.moveToNext()) list.add(cursorTo(c));
            c.close();
        }
        return list;
    }

    public List<UserResource> getByType(int type) {
        return getByFolderAndType(-2, type);
    }

    public List<UserResource> getReferencedFolders() {
        List<UserResource> list = new ArrayList<>();
        Cursor c = db.query(TABLE_NAME, null, "type=?", new String[]{String.valueOf(UserResource.TYPE_REFERENCED_FOLDER)}, null, null, "created_at DESC");
        if (c != null) {
            while (c.moveToNext()) list.add(cursorTo(c));
            c.close();
        }
        return list;
    }

    public List<UserResource> getByFolderAndType(long folderId, int type) {
        List<UserResource> list = new ArrayList<>();
        String selection;
        String[] args;

        if (folderId == -2 && type == -1) {
            selection = null;
            args = null;
        } else if (folderId == -2) {
            selection = "type=?";
            args = new String[]{String.valueOf(type)};
        } else if (type == -1) {
            selection = folderId == -1 ? "(folder_id IS NULL OR folder_id=-1)" : "folder_id=?";
            args = folderId >= 0 ? new String[]{String.valueOf(folderId)} : null;
        } else {
            String folderSel = folderId == -1 ? "(folder_id IS NULL OR folder_id=-1)" : "folder_id=?";
            selection = folderSel + " AND type=?";
            args = new String[]{folderId >= 0 ? String.valueOf(folderId) : "-1", String.valueOf(type)};
        }

        Cursor c = db.query(TABLE_NAME, null, selection, args, null, null, "created_at DESC");
        if (c != null) {
            while (c.moveToNext()) list.add(cursorTo(c));
            c.close();
        }
        return list;
    }

    /** Items at the library root: imported folders and files not inside another referenced folder. */
    public List<UserResource> getRootResources() {
        return getRootResources(SORT_DATE);
    }

    public List<UserResource> getRootResources(int sort) {
        List<UserResource> list = new ArrayList<>();
        Cursor c = db.query(TABLE_NAME, null, "(parent_id=0 OR parent_id IS NULL)", null, null, null, orderClause(sort, true));
        if (c != null) {
            while (c.moveToNext()) list.add(cursorTo(c));
            c.close();
        }
        return list;
    }

    /** Direct children of a referenced folder (folders first, then files). */
    public List<UserResource> getChildren(long parentId) {
        return getChildren(parentId, SORT_DATE);
    }

    public List<UserResource> getChildren(long parentId, int sort) {
        List<UserResource> list = new ArrayList<>();
        Cursor c = db.query(TABLE_NAME, null, "parent_id=?", new String[]{String.valueOf(parentId)}, null, null,
                orderClause(sort, true));
        if (c != null) {
            while (c.moveToNext()) list.add(cursorTo(c));
            c.close();
        }
        return list;
    }

    private String orderClause(int sort, boolean foldersFirst) {
        String folders = foldersFirst ? "CASE WHEN type=1 THEN 0 ELSE 1 END," : "";
        switch (sort) {
            case SORT_TYPE:
                return folders + " mime_type COLLATE NOCASE ASC, title COLLATE NOCASE ASC";
            case SORT_SIZE:
                return folders + " file_size DESC, title COLLATE NOCASE ASC";
            case SORT_DATE:
                return folders + " created_at DESC";
            case SORT_NAME:
            default:
                return folders + " title COLLATE NOCASE ASC";
        }
    }

    public void moveToFolder(long id, long folderId) {
        ContentValues cv = new ContentValues();
        cv.put("folder_id", folderId);
        db.update(TABLE_NAME, cv, "_id=?", new String[]{String.valueOf(id)});
    }

    public void rename(long id, String title) {
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        db.update(TABLE_NAME, cv, "_id=?", new String[]{String.valueOf(id)});
    }

    public void deleteById(long id) {
        db.delete(TABLE_NAME, "_id=?", new String[]{String.valueOf(id)});
    }

    /** Removes the node and all of its descendants from the library (references only). */
    public void deleteSubtree(long id) {
        Set<Long> ids = new HashSet<>();
        collectDescendants(id, ids);
        ids.add(id);
        for (Long i : ids) {
            db.delete(TABLE_NAME, "_id=?", new String[]{String.valueOf(i)});
        }
    }

    private void collectDescendants(long parentId, Set<Long> out) {
        List<UserResource> children = getChildren(parentId);
        for (UserResource c : children) {
            collectDescendants(c.getId(), out);
            out.add(c.getId());
        }
    }

    public UserResource getById(long id) {
        Cursor c = db.query(TABLE_NAME, null, "_id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (c == null) return null;
        try {
            if (c.moveToFirst()) {
                return cursorTo(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    public long getIdByUri(String uri, int type) {
        Cursor c = db.query(TABLE_NAME, new String[]{"_id"}, "uri=? AND type=?", new String[]{uri, String.valueOf(type)}, null, null, null);
        long id = -1;
        if (c != null) {
            try {
                if (c.moveToFirst()) id = c.getLong(0);
            } finally {
                c.close();
            }
        }
        return id;
    }

    public int countByFolder(long folderId) {
        String selection = folderId == -1 ? "(folder_id IS NULL OR folder_id=-1)" : "folder_id=?";
        String[] args = folderId >= 0 ? new String[]{String.valueOf(folderId)} : null;
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + selection, args);
        int count = 0;
        if (c != null && c.moveToFirst()) { count = c.getInt(0); c.close(); }
        return count;
    }

    /** Retorna {subpastas, arquivos} filhos diretos de uma pasta referenciada. */
    public int[] countChildren(long parentId) {
        int[] result = {0, 0};
        Cursor c = db.rawQuery(
                "SELECT type, COUNT(*) FROM " + TABLE_NAME + " WHERE parent_id=? GROUP BY type",
                new String[]{String.valueOf(parentId)});
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    int type = c.getInt(0);
                    int count = c.getInt(1);
                    if (type == UserResource.TYPE_REFERENCED_FOLDER) {
                        result[0] += count;
                    } else {
                        result[1] += count;
                    }
                }
            } finally {
                c.close();
            }
        }
        return result;
    }

    public boolean existsUri(String uri, int type) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE uri=? AND type=?", new String[]{uri, String.valueOf(type)});
        int count = 0;
        if (c != null && c.moveToFirst()) { count = c.getInt(0); c.close(); }
        return count > 0;
    }

    /**
     * Imports a whole directory tree from the device into the library, preserving the
     * folder hierarchy (root folder + subfolders + files) with their real names.
     * The root folder is stored with the given parentId (0 = library root).
     *
     * @return the database id of the imported root folder, or -1 on failure.
     */
    public long importFolderTree(ContentResolver cr, Uri treeUri, long parentId) {
        try {
            String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId);
            String title = queryDisplayName(cr, documentUri);
            if (title == null || title.isEmpty()) title = lastSegment(treeUri);

            long existing = getIdByUri(treeUri.toString(), UserResource.TYPE_REFERENCED_FOLDER);
            long id;
            if (existing > 0) {
                id = existing;
                updateParent(id, parentId);
            } else {
                UserResource folder = new UserResource();
                folder.setTitle(title);
                folder.setUri(treeUri.toString());
                folder.setMimeType(DocumentsContract.Document.MIME_TYPE_DIR);
                folder.setType(UserResource.TYPE_REFERENCED_FOLDER);
                folder.setParentId(parentId);
                folder.setCreatedAt(System.currentTimeMillis());
                id = insert(folder);
            }
            if (id > 0) {
                importChildren(cr, treeUri, treeDocId, id, 0);
            }
            return id;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Imports the direct children (and subfolders recursively) of an already
     * registered referenced folder. Used to materialize legacy folders and during
     * tree navigation. Existing rows are reused, never duplicated.
     */
    public int importChildren(ContentResolver cr, Uri treeUri, String docId, long parentId, int depth) {
        if (depth > MAX_IMPORT_DEPTH) return 0;
        int count = 0;
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId);
        Cursor c = null;
        try {
            c = cr.query(childrenUri,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE,
                            DocumentsContract.Document.COLUMN_SIZE},
                    null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String childDocId = c.getString(0);
                    String name = c.getString(1);
                    String mime = c.getString(2);
                    long size = c.getLong(3);
                    Uri childDoc = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId);

                    if (mime != null && mime.contains(DocumentsContract.Document.MIME_TYPE_DIR)) {
                        long existing = getIdByUri(childDoc.toString(), UserResource.TYPE_REFERENCED_FOLDER);
                        long childId;
                        if (existing > 0) {
                            childId = existing;
                            updateParent(childId, parentId);
                        } else {
                            childId = insertFolder(cr, childDoc, name, mime, size, parentId);
                        }
                        if (childId > 0) {
                            count += importChildren(cr, treeUri, childDocId, childId, depth + 1);
                            count++;
                        }
                    } else {
                        long existing = getIdByUri(childDoc.toString(), UserResource.TYPE_REFERENCED_FILE);
                        if (existing <= 0) {
                            UserResource child = new UserResource();
                            child.setTitle(name != null && !name.isEmpty() ? name : "Sem nome");
                            child.setUri(childDoc.toString());
                            child.setMimeType(mime != null ? mime : "application/octet-stream");
                            child.setSize(size);
                            child.setType(UserResource.TYPE_REFERENCED_FILE);
                            child.setParentId(parentId);
                            child.setCreatedAt(System.currentTimeMillis());
                            insert(child);
                            count++;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return count;
    }

    private long insertFolder(ContentResolver cr, Uri docUri, String name, String mime, long size, long parentId) {
        String title = name;
        if (title == null || title.isEmpty()) title = queryDisplayName(cr, docUri);
        if (title == null || title.isEmpty()) title = lastSegment(docUri);

        UserResource child = new UserResource();
        child.setTitle(title);
        child.setUri(docUri.toString());
        child.setMimeType(mime != null ? mime : DocumentsContract.Document.MIME_TYPE_DIR);
        child.setSize(size);
        child.setType(UserResource.TYPE_REFERENCED_FOLDER);
        child.setParentId(parentId);
        child.setCreatedAt(System.currentTimeMillis());
        return insert(child);
    }

    public void updateParent(long id, long parentId) {
        ContentValues cv = new ContentValues();
        cv.put("parent_id", parentId);
        db.update(TABLE_NAME, cv, "_id=?", new String[]{String.valueOf(id)});
    }

    /** Retorna o Uri da árvore raiz que contém o nó (a raiz guarda o tree Uri). */
    public Uri getTreeUri(long nodeId) {
        long cur = nodeId;
        while (cur > 0) {
            UserResource n = getById(cur);
            if (n == null) return null;
            if (n.getParentId() <= 0) return Uri.parse(n.getUri());
            cur = n.getParentId();
        }
        return null;
    }

    /**
     * Garante que os filhos diretos de uma pasta referenciada estão persistidos.
     * Usado ao navegar: se a pasta está vazia no banco, materializa da fonte (SAF),
     * reaproveitando linhas existentes (idempotente). Retorna quantos itens entrou.
     */
    public int importChildrenForFolder(long folderId, ContentResolver cr) {
        UserResource folder = getById(folderId);
        if (folder == null) return 0;
        Uri rootTree = getTreeUri(folderId);
        if (rootTree == null) return 0;
        try {
            String docId;
            if (folder.getParentId() <= 0) {
                docId = DocumentsContract.getTreeDocumentId(Uri.parse(folder.getUri()));
            } else {
                docId = DocumentsContract.getDocumentId(Uri.parse(folder.getUri()));
            }
            return importChildren(cr, rootTree, docId, folderId, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    private String queryDisplayName(ContentResolver cr, Uri documentUri) {
        String name = null;
        Cursor c = null;
        try {
            c = cr.query(documentUri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) {
                name = c.getString(0);
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return name;
    }

    private String lastSegment(Uri uri) {
        String s = uri.getLastPathSegment();
        if (s == null) return "Pasta";
        int idx = s.indexOf(':');
        if (idx >= 0) s = s.substring(idx + 1);
        return s.isEmpty() ? "Pasta" : s;
    }

    private UserResource cursorTo(Cursor c) {
        UserResource r = new UserResource();
        r.setId(c.getLong(c.getColumnIndexOrThrow("_id")));
        r.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
        r.setUri(c.getString(c.getColumnIndexOrThrow("uri")));
        r.setMimeType(c.getString(c.getColumnIndexOrThrow("mime_type")));
        r.setSize(c.getLong(c.getColumnIndexOrThrow("file_size")));
        if (c.getColumnIndex("folder_id") >= 0) r.setFolderId(c.getLong(c.getColumnIndexOrThrow("folder_id")));
        if (c.getColumnIndex("type") >= 0) r.setType(c.getInt(c.getColumnIndexOrThrow("type")));
        if (c.getColumnIndex("parent_id") >= 0) r.setParentId(c.getLong(c.getColumnIndexOrThrow("parent_id")));
        r.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        return r;
    }
}
