import subprocess, os, sys

def pull_via_runas(pkg, dbname, out_path):
    cmd = ["adb", "exec-out", "run-as", pkg, "cat", "databases/%s" % dbname]
    data = subprocess.run(cmd, capture_output=True).stdout
    bom = b'\xff\xfe'
    if data[:2] == bom:
        # UTF-16LE BOM: strip BOM, zip every 2 bytes
        raw = data[2:]
        if len(raw) % 2 == 1:
            raw = raw[:-1]
        data = bytes(b for b in raw[0::2])
    with open(out_path, "wb") as f:
        f.write(data)
    return len(data)

pkg = "com.biblia.estudo"
base = os.path.join(os.path.dirname(__file__), "pulled")
os.makedirs(base, exist_ok=True)
for name in ["biblia_estudo.db", "biblia_ara.db"]:
    out = os.path.join(base, name)
    if os.path.exists(out):
        os.remove(out)
    try:
        n = pull_via_runas(pkg, name, out)
        print(name, "->", n, "bytes")
    except Exception as e:
        print(name, "ERRO", e)