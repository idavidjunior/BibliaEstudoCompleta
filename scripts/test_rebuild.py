import json, os, sqlite3
BASE = os.path.join(os.path.dirname(__file__), "..", "assets", "databases")
DB_PATH = os.path.join(BASE, "biblia_estudo.db")
con = sqlite3.connect(DB_PATH)
cur = con.cursor()
print("ANTES:")
print(cur.execute("SELECT sql FROM sqlite_master WHERE name='verses'").fetchone()[0])

cur.execute("CREATE TABLE verses_new ("
            "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "book_id INTEGER NOT NULL,"
            "chapter INTEGER NOT NULL,"
            "verse_number INTEGER NOT NULL,"
            "text TEXT NOT NULL,"
            "hebrew_text TEXT,"
            "greek_text TEXT,"
            "has_commentary INTEGER DEFAULT 0,"
            "has_cross_ref INTEGER DEFAULT 0,"
            "translation_id TEXT NOT NULL DEFAULT 'arc',"
            "FOREIGN KEY (book_id) REFERENCES books(_id),"
            "UNIQUE(book_id, chapter, verse_number, translation_id))")
cur.execute("INSERT INTO verses_new SELECT * FROM verses")
cur.execute("DROP TABLE verses")
cur.execute("ALTER TABLE verses_new RENAME TO verses")
print("DEPOIS:")
print(cur.execute("SELECT sql FROM sqlite_master WHERE name='verses'").fetchone()[0])
print(cur.execute("SELECT COUNT(*) FROM verses").fetchone()[0])
con.commit()
con.close()