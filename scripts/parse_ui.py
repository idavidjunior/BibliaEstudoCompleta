import re, os
p = os.path.join(os.environ["TEMP"], "ui2.xml")
data = open(p, encoding="utf-8").read()
for m in re.finditer(r'text="([^"]+)"[^>]*bounds="(\[[^\"]+\])"', data):
    t, b = m.group(1), m.group(2)
    if t.strip():
        print(t, "->", b)