package com.biblia.estudo.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.biblia.estudo.model.Group;

import java.util.ArrayList;
import java.util.List;

public class GroupDao {

    private SQLiteDatabase db;

    public GroupDao(SQLiteDatabase db) {
        this.db = db;
    }

    public long insert(Group group) {
        ContentValues cv = new ContentValues();
        cv.put("name", group.getName());
        cv.put("type", group.getType());
        cv.put("parent_id", group.getParentId());
        cv.put("group_order", group.getOrder());
        cv.put("created_at", System.currentTimeMillis());
        cv.put("color", group.getColor());
        return db.insert(BibleDatabaseHelper.TABLE_GROUPS, null, cv);
    }

    public int update(Group group) {
        ContentValues cv = new ContentValues();
        cv.put("name", group.getName());
        cv.put("group_order", group.getOrder());
        cv.put("color", group.getColor());
        return db.update(BibleDatabaseHelper.TABLE_GROUPS, cv, "_id=?", new String[]{String.valueOf(group.getId())});
    }

    public int delete(long groupId) {
        // Move items in this group back to root (group_id = 0)
        ContentValues cv = new ContentValues();
        cv.put("group_id", 0);
        db.update(BibleDatabaseHelper.TABLE_HIGHLIGHTS, cv, "group_id=?", new String[]{String.valueOf(groupId)});
        db.update(BibleDatabaseHelper.TABLE_FAVORITES, cv, "group_id=?", new String[]{String.valueOf(groupId)});

        // Delete group
        return db.delete(BibleDatabaseHelper.TABLE_GROUPS, "_id=?", new String[]{String.valueOf(groupId)});
    }

    public List<Group> getAll(int type) {
        List<Group> list = new ArrayList<>();
        Cursor c = db.query(BibleDatabaseHelper.TABLE_GROUPS, null,
                "type=?", new String[]{String.valueOf(type)},
                null, null, "group_order ASC, name ASC");
        if (c != null) {
            while (c.moveToNext()) list.add(cursorToGroup(c));
            c.close();
        }
        return list;
    }

    public Group getById(long groupId) {
        Cursor c = db.query(BibleDatabaseHelper.TABLE_GROUPS, null,
                "_id=?", new String[]{String.valueOf(groupId)},
                null, null, null);
        Group group = null;
        if (c != null && c.moveToFirst()) {
            group = cursorToGroup(c);
            c.close();
        }
        return group;
    }

    public int getCount(int type) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + BibleDatabaseHelper.TABLE_GROUPS + " WHERE type=?",
                new String[]{String.valueOf(type)});
        int count = 0;
        if (c != null && c.moveToFirst()) { count = c.getInt(0); c.close(); }
        return count;
    }

    private Group cursorToGroup(Cursor c) {
        Group g = new Group();
        g.setId(c.getLong(c.getColumnIndexOrThrow("_id")));
        g.setName(c.getString(c.getColumnIndexOrThrow("name")));
        g.setType(c.getInt(c.getColumnIndexOrThrow("type")));
        g.setParentId(c.getLong(c.getColumnIndexOrThrow("parent_id")));
        g.setOrder(c.getInt(c.getColumnIndexOrThrow("group_order")));
        g.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        g.setColor(c.getInt(c.getColumnIndexOrThrow("color")));
        return g;
    }
}