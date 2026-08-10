package com.biblia.estudo.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.biblia.estudo.data.BibleDatabaseHelper;
import com.biblia.estudo.data.DatabaseManager;
import com.biblia.estudo.model.Verse;

import java.util.ArrayList;
import java.util.List;

public class SearchEngine {

    private static final String TABLE_FTS = "verses_fts";

    private SQLiteDatabase db;

    public SearchEngine(DatabaseManager dbManager) {
        this.db = dbManager.getBibleDatabase();
    }

    public List<Verse> searchByWord(String word) {
        List<Verse> results = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT v.*, b.name as book_name FROM " + BibleDatabaseHelper.TABLE_VERSES + " v " +
                        "JOIN " + BibleDatabaseHelper.TABLE_BOOKS + " b ON v.book_id = b._id " +
                        "WHERE v.text LIKE ? ORDER BY b.book_order, v.chapter, v.verse_number LIMIT 200",
                new String[]{"%" + word.trim() + "%"});
        if (c != null) {
            while (c.moveToNext()) {
                results.add(cursorToVerse(c));
            }
            c.close();
        }
        return results;
    }

    public Cursor searchByWordCursor(String word) {
        String match = SearchUtils.wordMatch(word);
        if (match.isEmpty()) return null;
        try {
            return db.rawQuery(ftsSelect() + " WHERE v.rowid IN " +
                            "(SELECT rowid FROM " + TABLE_FTS + " WHERE " + TABLE_FTS + " MATCH ?) " +
                            "ORDER BY b.book_order, v.chapter, v.verse_number LIMIT 200",
                    new String[]{match});
        } catch (Exception e) {
            return fallbackByText(word);
        }
    }

    public Cursor searchByPhraseCursor(String phrase) {
        String match = SearchUtils.phraseMatch(phrase);
        if (match.isEmpty()) return null;
        try {
            return db.rawQuery(ftsSelect() + " WHERE v.rowid IN " +
                            "(SELECT rowid FROM " + TABLE_FTS + " WHERE " + TABLE_FTS + " MATCH ?) " +
                            "ORDER BY b.book_order, v.chapter, v.verse_number LIMIT 200",
                    new String[]{match});
        } catch (Exception e) {
            return fallbackByText(phrase);
        }
    }

    public Cursor searchByBookAndWord(long bookId, String word) {
        return db.rawQuery(
                ftsSelect() + " WHERE v.book_id=? AND v.text LIKE ? " +
                        "ORDER BY v.chapter, v.verse_number LIMIT 200",
                new String[]{String.valueOf(bookId), "%" + word.trim() + "%"});
    }

    public Cursor searchByBookCursor(String bookName) {
        String norm = SearchUtils.normalize(bookName);
        if (norm.isEmpty()) return null;
        List<Long> ids = new ArrayList<>();
        Cursor b = db.rawQuery("SELECT _id, name FROM " + BibleDatabaseHelper.TABLE_BOOKS, null);
        if (b != null) {
            while (b.moveToNext()) {
                String name = b.getString(1);
                if (name != null && SearchUtils.normalize(name).contains(norm)) {
                    ids.add(b.getLong(0));
                }
            }
            b.close();
        }
        if (ids.isEmpty()) return null;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) placeholders.append(',');
            placeholders.append('?');
        }
        String[] args = new String[ids.size()];
        for (int i = 0; i < ids.size(); i++) args[i] = String.valueOf(ids.get(i));
        return db.rawQuery(ftsSelect() +
                        "WHERE v.book_id IN (" + placeholders + ") " +
                        "ORDER BY b.book_order, v.chapter, v.verse_number LIMIT 200",
                args);
    }

    public Cursor searchByTopic(String topic) {
        String match = SearchUtils.wordMatch(topic);
        if (match.isEmpty()) return null;
        try {
            return db.rawQuery(ftsSelect() + " WHERE v.rowid IN " +
                            "(SELECT rowid FROM " + TABLE_FTS + " WHERE " + TABLE_FTS + " MATCH ?) " +
                            "ORDER BY b.book_order, v.chapter, v.verse_number LIMIT 200",
                    new String[]{match});
        } catch (Exception e) {
            return fallbackByText(topic);
        }
    }

    public List<String> getSuggestions(String prefix) {
        List<String> suggestions = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT DISTINCT b.name FROM " + BibleDatabaseHelper.TABLE_BOOKS + " b " +
                        "WHERE b.name LIKE ? LIMIT 10",
                new String[]{"%" + prefix + "%"});
        if (c != null) {
            while (c.moveToNext()) {
                suggestions.add(c.getString(0));
            }
            c.close();
        }
        return suggestions;
    }

    public Cursor searchApocrypha(String query) {
        return db.rawQuery(
                "SELECT c._id, c.book_id, c.chapter_number as chapter, c.title, c.content as text, " +
                        "b.name as book_name " +
                        "FROM apocrypha_chapters c " +
                        "JOIN apocrypha_books b ON c.book_id = b._id " +
                        "WHERE c.content LIKE ? OR c.title LIKE ? " +
                        "ORDER BY b.book_order, c.chapter_number LIMIT 200",
                new String[]{"%" + query.trim() + "%", "%" + query.trim() + "%"});
    }

    private String ftsSelect() {
        return "SELECT v._id, v.book_id, v.chapter, v.verse_number, v.text, " +
                "b.name as book_name, b.chapter_count " +
                "FROM " + BibleDatabaseHelper.TABLE_VERSES + " v " +
                "JOIN " + BibleDatabaseHelper.TABLE_BOOKS + " b ON v.book_id = b._id ";
    }

    private Cursor fallbackByText(String word) {
        return db.rawQuery(ftsSelect() +
                        "WHERE v.text LIKE ? ORDER BY b.book_order, v.chapter, v.verse_number LIMIT 200",
                new String[]{"%" + word.trim() + "%"});
    }

    private Verse cursorToVerse(Cursor c) {
        Verse v = new Verse();
        v.setId(c.getLong(c.getColumnIndexOrThrow("_id")));
        v.setBookId(c.getLong(c.getColumnIndexOrThrow("book_id")));
        v.setChapter(c.getInt(c.getColumnIndexOrThrow("chapter")));
        v.setVerseNumber(c.getInt(c.getColumnIndexOrThrow("verse_number")));
        v.setText(c.getString(c.getColumnIndexOrThrow("text")));
        return v;
    }
}
