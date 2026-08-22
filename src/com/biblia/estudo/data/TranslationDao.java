package com.biblia.estudo.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.biblia.estudo.model.Translation;

import java.util.ArrayList;
import java.util.List;

public class TranslationDao {

    private SQLiteDatabase db;

    public TranslationDao(SQLiteDatabase db) {
        this.db = db;
    }

    public static final String TABLE_TRANSLATIONS = "translations";
    public static final String DEFAULT_TRANSLATION = "arc";

    public List<Translation> getAll() {
        List<Translation> list = new ArrayList<>();
        Cursor c = db.query(TABLE_TRANSLATIONS, null, null, null, null, null, "_id ASC");
        if (c != null) {
            while (c.moveToNext()) {
                list.add(cursorToTranslation(c));
            }
            c.close();
        }
        return list;
    }

    public Translation getByCode(String code) {
        Cursor c = db.query(TABLE_TRANSLATIONS, null, "code=?", new String[]{code}, null, null, null);
        if (c != null && c.moveToFirst()) {
            Translation t = cursorToTranslation(c);
            c.close();
            return t;
        }
        return null;
    }

    public String getName(String code) {
        Translation t = getByCode(code);
        return t != null ? t.getName() : code;
    }

    public boolean exists(String code) {
        Cursor c = db.query(TABLE_TRANSLATIONS, new String[]{"_id"}, "code=?",
                new String[]{code}, null, null, null);
        boolean exists = c != null && c.getCount() > 0;
        if (c != null) c.close();
        return exists;
    }

    private Translation cursorToTranslation(Cursor c) {
        Translation t = new Translation();
        t.setId(c.getLong(c.getColumnIndexOrThrow("_id")));
        t.setCode(c.getString(c.getColumnIndexOrThrow("code")));
        t.setName(c.getString(c.getColumnIndexOrThrow("name")));
        int li = c.getColumnIndex("language");
        if (li >= 0) t.setLanguage(c.getString(li));
        int lci = c.getColumnIndex("license");
        if (lci >= 0) t.setLicense(c.getString(lci));
        int di = c.getColumnIndex("is_default");
        if (di >= 0) t.setDefault(c.getInt(di) > 0);
        return t;
    }
}