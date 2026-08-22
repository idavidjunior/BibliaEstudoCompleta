package com.biblia.estudo.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.biblia.estudo.model.ReferenceMap;

import java.util.ArrayList;
import java.util.List;

public class ReferenceMapDao {

    private SQLiteDatabase db;

    public ReferenceMapDao(SQLiteDatabase db) {
        this.db = db;
    }

    public long insert(ReferenceMap map) {
        ContentValues cv = new ContentValues();
        cv.put("title", map.getTitle());
        cv.put("description", map.getDescription());
        long now = System.currentTimeMillis();
        cv.put("created_at", now);
        cv.put("updated_at", now);
        cv.put("node_count", 0);
        cv.put("edge_count", 0);
        return db.insert(ReferenceMapDatabaseHelper.TABLE_MAPS, null, cv);
    }

    public int update(ReferenceMap map) {
        ContentValues cv = new ContentValues();
        cv.put("title", map.getTitle());
        cv.put("description", map.getDescription());
        cv.put("updated_at", System.currentTimeMillis());
        cv.put("node_count", map.getNodeCount());
        cv.put("edge_count", map.getEdgeCount());
        return db.update(ReferenceMapDatabaseHelper.TABLE_MAPS, cv, "_id=?", new String[]{String.valueOf(map.getId())});
    }

    public int delete(long mapId) {
        return db.delete(ReferenceMapDatabaseHelper.TABLE_MAPS, "_id=?", new String[]{String.valueOf(mapId)});
    }

    public ReferenceMap getById(long id) {
        Cursor c = db.query(ReferenceMapDatabaseHelper.TABLE_MAPS, null, "_id=?", new String[]{String.valueOf(id)}, null, null, null);
        ReferenceMap map = null;
        if (c != null && c.moveToFirst()) {
            map = cursorToMap(c);
            c.close();
        }
        return map;
    }

    public List<ReferenceMap> getAll() {
        List<ReferenceMap> list = new ArrayList<>();
        Cursor c = db.query(ReferenceMapDatabaseHelper.TABLE_MAPS, null, null, null, null, null, "updated_at DESC");
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToMap(c));
            }
            c.close();
        }
        return list;
    }

    public int updateCounts(long mapId, int nodeCount, int edgeCount) {
        ContentValues cv = new ContentValues();
        cv.put("node_count", nodeCount);
        cv.put("edge_count", edgeCount);
        cv.put("updated_at", System.currentTimeMillis());
        return db.update(ReferenceMapDatabaseHelper.TABLE_MAPS, cv, "_id=?", new String[]{String.valueOf(mapId)});
    }

    private ReferenceMap cursorToMap(Cursor c) {
        ReferenceMap map = new ReferenceMap();
        map.setId(c.getLong(c.getColumnIndexOrThrow("_id")));
        map.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
        map.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
        map.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        map.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow("updated_at")));
        map.setNodeCount(c.getInt(c.getColumnIndexOrThrow("node_count")));
        map.setEdgeCount(c.getInt(c.getColumnIndexOrThrow("edge_count")));
        return map;
    }
}