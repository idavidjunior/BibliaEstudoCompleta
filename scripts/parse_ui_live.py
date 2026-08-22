import subprocess, re, sys
out = subprocess.run(["adb", "exec-out", "uiautomator", "dump", "/dev/tty"], capture_output=True)
data = out.stdout.decode("utf-8", "replace")
m = re.search(r"<hierarchy.*?</hierarchy>", data, re.S)
if not m:
    print("SEM HIERARQUIA")
    sys.exit(0)
xml = m.group(0)
print("TAM:", len(xml))
for t, a, b, c, d in re.findall(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    if t.strip():
        print(t, "->", a, b, c, d)