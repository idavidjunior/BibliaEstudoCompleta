package com.biblia.estudo.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.biblia.estudo.model.UserResource;

import java.util.ArrayList;
import java.util.List;

public class UserResourceDao {

    private static final String TABLE_NAME = "user_resources";
    private SQLiteDatabase db;

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
        try { db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_uri_type ON " + TABLE_NAME + " (uri, type)"); } catch (Exception ignored) {}
    }

    public long insert(UserResource res) {
        ContentValues cv = new ContentValues();
        cv.put("title", res.getTitle());
        cv.put("uri", res.getUri());
        cv.put("mime_type", res.getMimeType());
        cv.put("file_size", res.getSize());
        cv.put("folder_id", res.getFolderId());
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

    public void moveToFolder(long id, long folderId) {
        ContentValues cv = new ContentValues();
        cv.put("folder_id", folderId);
        db.update(TABLE_NAME, cv, "_id=?", new String[]{String.valueOf(id)});
    }

    public void deleteById(long id) {
        db.delete(TABLE_NAME, "_id=?", new String[]{String.valueOf(id)});
    }

    public UserResource getById(long id) {
        Cursor c = db.query(TABLE_NAME, null, "_id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (c != null && c.moveToFirst()) {
            UserResource r = cursorTo(c);
            c.close();
            return r;
        }
        return null;
    }

    public int countByFolder(long folderId) {
        String selection = folderId == -1 ? "(folder_id IS NULL OR folder_id=-1)" : "folder_id=?";
        String[] args = folderId >= 0 ? new String[]{String.valueOf(folderId)} : null;
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + selection, args);
        int count = 0;
        if (c != null && c.moveToFirst()) { count = c.getInt(0); c.close(); }
        return count;
    }

    public boolean existsUri(String uri, int type) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE uri=? AND type=?", new String[]{uri, String.valueOf(type)});
        int count = 0;
        if (c != null && c.moveToFirst()) { count = c.getInt(0); c.close(); }
        return count > 0;
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
        r.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        return r;
    }
}
