package com.biblia.estudo.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ReferenceMapDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "referencias_cruzadas.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_MAPS = "reference_maps";
    public static final String TABLE_NODES = "reference_nodes";
    public static final String TABLE_EDGES = "reference_edges";

    public ReferenceMapDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_MAPS + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "description TEXT," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL," +
                "node_count INTEGER DEFAULT 0," +
                "edge_count INTEGER DEFAULT 0" +
                ");");

        db.execSQL("CREATE TABLE " + TABLE_NODES + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "map_id INTEGER NOT NULL," +
                "book_id INTEGER NOT NULL," +
                "book_name TEXT NOT NULL," +
                "chapter INTEGER NOT NULL," +
                "verse INTEGER NOT NULL," +
                "verse_text TEXT," +
                "translation TEXT," +
                "color TEXT," +
                "note TEXT," +
                "position_x REAL DEFAULT 0," +
                "position_y REAL DEFAULT 0," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL," +
                "is_expanded INTEGER DEFAULT 1," +
                "FOREIGN KEY(map_id) REFERENCES " + TABLE_MAPS + "(_id) ON DELETE CASCADE" +
                ");");

        db.execSQL("CREATE TABLE " + TABLE_EDGES + " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "map_id INTEGER NOT NULL," +
                "source_node_id INTEGER NOT NULL," +
                "target_node_id INTEGER NOT NULL," +
                "relation_type TEXT NOT NULL," +
                "note TEXT," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL," +
                "FOREIGN KEY(map_id) REFERENCES " + TABLE_MAPS + "(_id) ON DELETE CASCADE," +
                "FOREIGN KEY(source_node_id) REFERENCES " + TABLE_NODES + "(_id) ON DELETE CASCADE," +
                "FOREIGN KEY(target_node_id) REFERENCES " + TABLE_NODES + "(_id) ON DELETE CASCADE" +
                ");");

        db.execSQL("CREATE INDEX idx_nodes_map ON " + TABLE_NODES + "(map_id);");
        db.execSQL("CREATE INDEX idx_nodes_reference ON " + TABLE_NODES + "(book_id, chapter, verse);");
        db.execSQL("CREATE INDEX idx_edges_map ON " + TABLE_EDGES + "(map_id);");
        db.execSQL("CREATE INDEX idx_edges_source ON " + TABLE_EDGES + "(source_node_id);");
        db.execSQL("CREATE INDEX idx_edges_target ON " + TABLE_EDGES + "(target_node_id);");
        db.execSQL("CREATE UNIQUE INDEX idx_edges_unique ON " + TABLE_EDGES + "(source_node_id, target_node_id);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Future migrations
        }
    }
}