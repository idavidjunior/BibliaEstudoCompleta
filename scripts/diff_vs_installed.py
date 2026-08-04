import os, re, sys

def norm(s):
    import re
    # collapse whitespace and strip jadx noise lines
    s = re.sub(r"/\* JADX.*?\*/\s*", "", s)
    s = re.sub(r"\s+", " ", s)
    return s.strip()

PAS = os.path.join(os.path.dirname(__file__), "..", "src")
DEC = os.path.join(os.path.dirname(__file__), "pulled", "decompiled", "src_out", "sources")

# NOTE: we pass decompiled path via env to avoid duplicating; compute from default temp too
import glob
cand = [DEC]
tl = os.environ.get("TEMP", "")
c = os.path.join(tl, "biblia", "src_out", "sources")
if os.path.isdir(c):
    cand = [c]

def walk(base):
    out = {}
    for root, _, files in os.walk(base):
        for f in files:
            if f.endswith(".java"):
                full = os.path.join(root, f)
                rel = os.path.relpath(full, base)
                out[rel] = full
    return out

decroot = next(c for c in cand if os.path.isdir(c))
inst = walk(decroot)
pasta = walk(PAS)

inst_rel = set(inst)
pasta_rel = set(pasta)

print("=== NOVOS no instalado (nao existem na pasta) ===")
for r in sorted(inst_rel - pasta_rel):
    print("  +", r)

print("\n=== AUSENTES da pasta (existem na pasta, nao no instalado) ===")
for r in sorted(pasta_rel - inst_rel):
    print("  -", r)

print("\n=== DIFERENTES (conteudo) no instalado vs pasta ===")
diff = []
for r in sorted(inst_rel & pasta_rel):
    a = open(inst[r], encoding="utf-8", errors="replace").read()
    b = open(pasta[r], encoding="utf-8", errors="replace").read()
    if norm(a) != norm(b):
        diff.append(r)
for r in diff:
    print("  ~", r)
print("\nTOTAIS: instalado=%d pasta=%d novos_inst=%d diff=%d" % (len(inst_rel), len(pasta_rel), len(inst_rel-pasta_rel), len(diff)))