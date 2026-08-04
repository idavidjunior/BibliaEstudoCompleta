import sqlite3, os
base = os.path.join(os.path.dirname(__file__), "pulled")
path = os.path.join(base, "biblia_estudo.db")
con = sqlite3.connect(path)
cur = con.cursor()
tables = [r[0] for r in cur.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name").fetchall()]
print("user_version:", cur.execute("PRAGMA user_version").fetchone()[0])
print("tabelas:", tables)
for t in tables:
    try:
        cols = [c[1] for c in cur.execute("PRAGMA table_info(%s)" % t).fetchall()]
        print("  %-20s %s" % (t, cols))
    except Exception as e:
        print("  %s ERRO %s" % (t, e))
con.close()