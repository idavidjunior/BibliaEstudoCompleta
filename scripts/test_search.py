import sqlite3

db = sqlite3.connect('assets/databases/biblia_estudo.db')
c = db.cursor()

c.execute("SELECT name, type FROM sqlite_master WHERE type IN ('table','index') AND name LIKE 'verse%'")
print('objects:', c.fetchall())

c.execute("SELECT count(*) FROM verses")
print('verses rows:', c.fetchone()[0])

c.execute("SELECT translation_id, count(*) FROM verses GROUP BY translation_id")
print('by translation:', c.fetchall())

c.execute("SELECT _id, translation_id, text FROM verses WHERE text LIKE '%creou%' LIMIT 5")
rows = c.fetchall()
print('creou rows:', len(rows))
for r in rows[:3]:
    print(r)

# test FTS query
try:
    c.execute("SELECT rowid FROM verses_fts WHERE verses_fts MATCH 'creou' LIMIT 5")
    print('fts creou:', c.fetchall())
except Exception as e:
    print('fts error:', e)

# test fts schema
c.execute("SELECT sql FROM sqlite_master WHERE name='verses_fts'")
print('fts schema:', c.fetchone()[0][:200])

# test count of fts rows
c.execute("SELECT count(*) FROM verses_fts")
print('fts rows:', c.fetchone()[0])

# full query as the app does (searchByWordCursor)
try:
    c.execute("SELECT v._id, v.book_id, v.chapter, v.verse_number, v.text, b.name as book_name, b.chapter_count "
              "FROM verses v JOIN books b ON v.book_id = b._id "
              "WHERE v.rowid IN (SELECT rowid FROM verses_fts WHERE verses_fts MATCH ?) AND v.translation_id = ? "
              "ORDER BY b.book_order, v.chapter, v.verse_number LIMIT 200", ('creou', 'alm1911'))
    rows = c.fetchall()
    print('app query alm1911 creou:', len(rows))
    for r in rows[:3]:
        print(r[4][:60], '|', r[5])
except Exception as e:
    print('app query error:', e)
db.close()