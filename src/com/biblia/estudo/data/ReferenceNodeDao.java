package com.biblia.estudo.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.biblia.estudo.model.ReferenceNode;

import java.util.ArrayList;
import java.util.List;

public class ReferenceNodeDao {

    private SQLiteDatabase db;

    public ReferenceNodeDao(SQLiteDatabase db) {
        this.db = db;
    }

    public long insert(ReferenceNode node) {
        ContentValues cv = new ContentValues();
        cv.put("map_id", node.getMapId());
        cv.put("book_id", node.getBookId());
        cv.put("book_name", node.getBookName());
        cv.put("chapter", node.getChapter());
        cv.put("verse", node.getVerse());
        cv.put("verse_text", node.getVerseText());
        cv.put("translation", node.getTranslation());
        cv.put("color", node.getColor());
        cv.put("note", node.getNote());
        cv.put("position_x", node.getPositionX());
        cv.put("position_y", node.getPositionY());
        long now = System.currentTimeMillis();
        cv.put("created_at", now);
        cv.put("updated_at", now);
        cv.put("is_expanded", node.isExpanded() ? 1 : 0);
        return db.insert(ReferenceMapDatabaseHelper.TABLE_NODES, null, cv);
    }

    public int update(ReferenceNode node) {
        ContentValues cv = new ContentValues();
        cv.put("book_id", node.getBookId());
        cv.put("book_name", node.getBookName());
        cv.put("chapter", node.getChapter());
        cv.put("verse", node.getVerse());
        cv.put("verse_text", node.getVerseText());
        cv.put("translation", node.getTranslation());
        cv.put("color", node.getColor());
        cv.put("note", node.getNote());
        cv.put("position_x", node.getPositionX());
        cv.put("position_y", node.getPositionY());
        cv.put("updated_at", System.currentTimeMillis());
        cv.put("is_expanded", node.isExpanded() ? 1 : 0);
        return db.update(ReferenceMapDatabaseHelper.TABLE_NODES, cv, "_id=?", new String[]{String.valueOf(node.getId())});
    }

    public int delete(long nodeId) {
        return db.delete(ReferenceMapDatabaseHelper.TABLE_NODES, "_id=?", new String[]{String.valueOf(nodeId)});
    }

    public ReferenceNode getById(long id) {
        Cursor c = db.query(ReferenceMapDatabaseHelper.TABLE_NODES, null, "_id=?", new String[]{String.valueOf(id)}, null, null, null);
        ReferenceNode node = null;
        if (c != null && c.moveToFirst()) {
            node = cursorToNode(c);
            c.close();
        }
        return node;
    }

    public List<ReferenceNode> getByMap(long mapId) {
        List<ReferenceNode> list = new ArrayList<>();
        Cursor c = db.query(ReferenceMapDatabaseHelper.TABLE_NODES, null, "map_id=?", new String[]{String.valueOf(mapId)}, null, null, "created_at ASC");
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToNode(c));
            }
            c.close();
        }
        return list;
    }

    public ReferenceNode findByReference(long mapId, long bookId, int chapter, int verse) {
        Cursor c = db.query(ReferenceMapDatabaseHelper.TABLE_NODES, null,
                "map_id=? AND book_id=? AND chapter=? AND verse=?",
                new String[]{String.valueOf(mapId), String.valueOf(bookId), String.valueOf(chapter), String.valueOf(verse)},
                null, null, null);
        ReferenceNode node = null;
        if (c != null && c.moveToFirst()) {
            node = cursorToNode(c);
            c.close();
        }
        return node;
    }

    public int getCountByMap(long mapId) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + ReferenceMapDatabaseHelper.TABLE_NODES + " WHERE map_id=?", new String[]{String.valueOf(mapId)});
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        return count;
    }

    public int updatePosition(long nodeId, float x, float y) {
        ContentValues cv = new ContentValues();
        cv.put("position_x", x);
        cv.put("position_y", y);
        cv.put("updated_at", System.currentTimeMillis());
        return db.update(ReferenceMapDatabaseHelper.TABLE_NODES, cv, "_id=?", new String[]{String.valueOf(nodeId)});
    }

    public int updateExpanded(long nodeId, boolean expanded) {
        ContentValues cv = new ContentValues();
        cv.put("is_expanded", expanded ? 1 : 0);
        cv.put("updated_at", System.currentTimeMillis());
        return db.update(ReferenceMapDatabaseHelper.TABLE_NODES, cv, "_id=?", new String[]{String.valueOf(nodeId)});
    }

    public int updateColor(long nodeId, String color) {
        ContentValues cv = new ContentValues();
        cv.put("color", color);
        cv.put("updated_at", System.currentTimeMillis());
        return db.update(ReferenceMapDatabaseHelper.TABLE_NODES, cv, "_id=?", new String[]{String.valueOf(nodeId)});
    }

    public int updateNote(long nodeId, String note) {
        ContentValues cv = new ContentValues();
        cv.put("note", note);
        cv.put("updated_at", System.currentTimeMillis());
        return db.update(ReferenceMapDatabaseHelper.TABLE_NODES, cv, "_id=?", new String[]{String.valueOf(nodeId)});
    }

    private ReferenceNode cursorToNode(Cursor c) {
        ReferenceNode node = new ReferenceNode();
        node.setId(c.getLong(c.getColumnIndexOrThrow("_id")));
        node.setMapId(c.getLong(c.getColumnIndexOrThrow("map_id")));
        node.setBookId(c.getLong(c.getColumnIndexOrThrow("book_id")));
        node.setBookName(c.getString(c.getColumnIndexOrThrow("book_name")));
        node.setChapter(c.getInt(c.getColumnIndexOrThrow("chapter")));
        node.setVerse(c.getInt(c.getColumnIndexOrThrow("verse")));
        node.setVerseText(c.getString(c.getColumnIndexOrThrow("verse_text")));
        node.setTranslation(c.getString(c.getColumnIndexOrThrow("translation")));
        node.setColor(c.getString(c.getColumnIndexOrThrow("color")));
        node.setNote(c.getString(c.getColumnIndexOrThrow("note")));
        node.setPositionX(c.getFloat(c.getColumnIndexOrThrow("position_x")));
        node.setPositionY(c.getFloat(c.getColumnIndexOrThrow("position_y")));
        node.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        node.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow("updated_at")));
        node.setExpanded(c.getInt(c.getColumnIndexOrThrow("is_expanded")) == 1);
        return node;
    }
}