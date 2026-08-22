package com.biblia.estudo.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.biblia.estudo.model.ReferenceEdge;

import java.util.ArrayList;
import java.util.List;

public class ReferenceEdgeDao {

    private SQLiteDatabase db;

    public ReferenceEdgeDao(SQLiteDatabase db) {
        this.db = db;
    }

    public long insert(ReferenceEdge edge) {
        ContentValues cv = new ContentValues();
        cv.put("map_id", edge.getMapId());
        cv.put("source_node_id", edge.getSourceNodeId());
        cv.put("target_node_id", edge.getTargetNodeId());
        cv.put("relation_type", edge.getRelationType());
        cv.put("note", edge.getNote());
        long now = System.currentTimeMillis();
        cv.put("created_at", now);
        cv.put("updated_at", now);
        return db.insert(ReferenceMapDatabaseHelper.TABLE_EDGES, null, cv);
    }

    public int update(ReferenceEdge edge) {
        ContentValues cv = new ContentValues();
        cv.put("relation_type", edge.getRelationType());
        cv.put("note", edge.getNote());
        cv.put("updated_at", System.currentTimeMillis());
        return db.update(ReferenceMapDatabaseHelper.TABLE_EDGES, cv, "_id=?", new String[]{String.valueOf(edge.getId())});
    }

    public int delete(long edgeId) {
        return db.delete(ReferenceMapDatabaseHelper.TABLE_EDGES, "_id=?", new String[]{String.valueOf(edgeId)});
    }

    public int deleteByNodes(long sourceNodeId, long targetNodeId) {
        return db.delete(ReferenceMapDatabaseHelper.TABLE_EDGES,
                "source_node_id=? AND target_node_id=?",
                new String[]{String.valueOf(sourceNodeId), String.valueOf(targetNodeId)});
    }

    public ReferenceEdge getById(long id) {
        Cursor c = db.query(ReferenceMapDatabaseHelper.TABLE_EDGES, null, "_id=?", new String[]{String.valueOf(id)}, null, null, null);
        ReferenceEdge edge = null;
        if (c != null && c.moveToFirst()) {
            edge = cursorToEdge(c);
            c.close();
        }
        return edge;
    }

    public List<ReferenceEdge> getByMap(long mapId) {
        List<ReferenceEdge> list = new ArrayList<>();
        Cursor c = db.query(ReferenceMapDatabaseHelper.TABLE_EDGES, null, "map_id=?", new String[]{String.valueOf(mapId)}, null, null, "created_at ASC");
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToEdge(c));
            }
            c.close();
        }
        return list;
    }

    public List<ReferenceEdge> getBySourceNode(long sourceNodeId) {
        List<ReferenceEdge> list = new ArrayList<>();
        Cursor c = db.query(ReferenceMapDatabaseHelper.TABLE_EDGES, null, "source_node_id=?", new String[]{String.valueOf(sourceNodeId)}, null, null, "created_at ASC");
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToEdge(c));
            }
            c.close();
        }
        return list;
    }

    public List<ReferenceEdge> getByTargetNode(long targetNodeId) {
        List<ReferenceEdge> list = new ArrayList<>();
        Cursor c = db.query(ReferenceMapDatabaseHelper.TABLE_EDGES, null, "target_node_id=?", new String[]{String.valueOf(targetNodeId)}, null, null, "created_at ASC");
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToEdge(c));
            }
            c.close();
        }
        return list;
    }

    public boolean existsConnection(long sourceNodeId, long targetNodeId) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + ReferenceMapDatabaseHelper.TABLE_EDGES +
                " WHERE source_node_id=? AND target_node_id=?", new String[]{String.valueOf(sourceNodeId), String.valueOf(targetNodeId)});
        boolean exists = false;
        if (c != null && c.moveToFirst()) {
            exists = c.getInt(0) > 0;
            c.close();
        }
        return exists;
    }

    public int getCountByMap(long mapId) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + ReferenceMapDatabaseHelper.TABLE_EDGES + " WHERE map_id=?", new String[]{String.valueOf(mapId)});
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        return count;
    }

    private ReferenceEdge cursorToEdge(Cursor c) {
        ReferenceEdge edge = new ReferenceEdge();
        edge.setId(c.getLong(c.getColumnIndexOrThrow("_id")));
        edge.setMapId(c.getLong(c.getColumnIndexOrThrow("map_id")));
        edge.setSourceNodeId(c.getLong(c.getColumnIndexOrThrow("source_node_id")));
        edge.setTargetNodeId(c.getLong(c.getColumnIndexOrThrow("target_node_id")));
        edge.setRelationType(c.getString(c.getColumnIndexOrThrow("relation_type")));
        edge.setNote(c.getString(c.getColumnIndexOrThrow("note")));
        edge.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        edge.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow("updated_at")));
        return edge;
    }
}