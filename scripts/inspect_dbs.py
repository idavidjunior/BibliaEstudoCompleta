import sqlite3, os

base = os.path.join(os.path.dirname(__file__), "..", "assets", "databases")
dbs = ["biblia_estudo.db", "comentarios.db", "dicionario.db", "referencias.db", "indices.db"]

for db in dbs:
    path = os.path.join(base, db)
    con = sqlite3.connect(path)
    cur = con.cursor()
    tables = [r[0] for r in cur.execute("SELECT name FROM sqlite_main WHERE type='table'".replace('_main','_master')).fetchall()]
    ver = cur.execute("PRAGMA user_version").fetchone()[0]
    print("=" * 60)
    print("DB:", db, "| size=%d bytes" % os.path.getsize(path))
    print("user_version:", ver)
    print("tabelas:", tables)
    for t in tables:
        try:
            cols = [c[1] for c in cur.execute("PRAGMA table_info(%s)" % t).fetchall()]
            cnt = cur.execute("SELECT COUNT(*) FROM %s" % t).fetchone()[0]
            print("   %-22s cols=%s rows=%s" % (t, cols, cnt))
        except Exception as e:
            print("   %s ERRO: %s" % (t, e))
    con.close()