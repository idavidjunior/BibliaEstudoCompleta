import json, os, shutil, sqlite3

BASE = os.path.join(os.path.dirname(__file__), "..", "assets", "databases")
DB_PATH = os.path.join(BASE, "biblia_estudo.db")
JSON_PATH = os.path.join(os.environ.get("TEMP", "/tmp"), "ALM1911.json")

BACKUP = DB_PATH + ".bak"
if not os.path.exists(BACKUP):
    shutil.copy2(DB_PATH, BACKUP)

def norm(s):
    import unicodedata
    return "".join(ch for ch in unicodedata.normalize("NFD", s) if not unicodedata.combining(ch)).lower()

con = sqlite3.connect(DB_PATH)
cur = con.cursor()

cols = [r[1] for r in cur.execute("PRAGMA table_info(verses)").fetchall()]
if "translation_id" not in cols:
    cur.execute("ALTER TABLE verses ADD COLUMN translation_id TEXT NOT NULL DEFAULT 'arc'")
    cur.execute("UPDATE verses SET translation_id = 'arc'")

# Remove old UNIQUE(book_id, chapter, verse_number) - rebuild verses table
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
cur.execute("CREATE INDEX IF NOT EXISTS idx_verses_book_chapter ON verses(book_id, chapter)")
cur.execute("CREATE INDEX IF NOT EXISTS idx_verses_text ON verses(text)")
cur.execute("CREATE INDEX IF NOT EXISTS idx_verses_chapter_verse ON verses(chapter, verse_number)")
cur.execute("CREATE INDEX IF NOT EXISTS idx_verses_translation ON verses(translation_id)")

cur.execute("CREATE TABLE IF NOT EXISTS translations ("
            "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "code TEXT NOT NULL UNIQUE,"
            "name TEXT NOT NULL,"
            "language TEXT,"
            "license TEXT,"
            "is_default INTEGER DEFAULT 0)")

cur.execute("SELECT COUNT(*) FROM translations")
if cur.execute("SELECT COUNT(*) FROM translations").fetchone()[0] == 0:
    cur.execute("INSERT INTO translations (code, name, language, license, is_default) VALUES (?,?,?,?,1)",
                ("arc", "Almeida Revista e Corrigida", "pt-BR", "Sociedade Bíblica do Brasil (licenciada)"))
    cur.execute("INSERT INTO translations (code, name, language, license, is_default) VALUES (?,?,?,?,0)",
                ("alm1911", "Almeida 1911", "pt-BR", "Domínio público"))

books = cur.execute("SELECT _id FROM books WHERE testament IN (1,2) ORDER BY book_order, _id").fetchall()
books = [r[0] for r in books]
print("Livros canonicos mapeados:", len(books))

data = json.load(open(JSON_PATH, encoding="utf-8"))
inserted = 0
skipped = 0
cur.execute("DELETE FROM verses WHERE translation_id = 'alm1911'")
for i, book in enumerate(data, start=1):
    if i > len(books):
        print("FORA DO RANGE:", i, book["abbrev"])
        skipped += 1
        continue
    book_id = books[i - 1]
    for ci, chapter in enumerate(book["chapters"], start=1):
        for vi, text in enumerate(chapter, start=1):
            if not text:
                continue
            cur.execute("INSERT INTO verses (book_id, chapter, verse_number, text, translation_id) VALUES (?,?,?,?,?)",
                        (book_id, ci, vi, text, "alm1911"))
            inserted += 1

cur.execute("DROP TABLE IF EXISTS verses_fts")
cur.execute('CREATE VIRTUAL TABLE verses_fts USING fts4(content="", text, book_id, chapter, verse_number)')
rows = cur.execute("SELECT rowid, text FROM verses").fetchall()
for rowid, text in rows:
    if not text:
        continue
    cur.execute("INSERT INTO verses_fts(docid, text, book_id, chapter, verse_number) VALUES (?,?,?,?,?)",
                (rowid, norm(text), None, None, None))

cur.execute("PRAGMA user_version = 7")
con.commit()

print("Livros mapeados:", len(books))
print("Versos ALM1911 inseridos:", inserted)
print("Abrevs nao mapeadas:", skipped)
print("Total versos no banco:", cur.execute("SELECT COUNT(*) FROM verses").fetchone()[0])
print("FTS rows:", cur.execute("SELECT COUNT(*) FROM verses_fts").fetchone()[0])
print("Traducoes:", cur.execute("SELECT code, name, is_default FROM translations").fetchall())
con.close()