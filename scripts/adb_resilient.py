#!/usr/bin/env python3
"""
Script de diagnóstico e instalação ADB resiliente.
Lida com: timeouts, reconexão, restart do servidor, e retry.
Uso: python scripts/adb_resilient.py install
      python scripts/adb_resilient.py status
      python scripts/adb_resilient.py logcat
"""
import subprocess
import sys
import time
import os

ADB_PATHS = [
    r"C:\Users\David Jr\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    "adb",
]
APK_PATH = os.path.join(os.path.dirname(__file__), "..", "bin", "BibliaEstudoCompleta.apk")
PACKAGE = "com.biblia.estudo"
TIMEOUT = 15

def find_adb():
    for p in ADB_PATHS:
        try:
            r = subprocess.run([p, "version"], capture_output=True, timeout=5)
            if r.returncode == 0:
                return p
        except:
            continue
    return None

def run_adb(adb, args, timeout=TIMEOUT, retries=3):
    for attempt in range(retries):
        try:
            r = subprocess.run([adb] + args, capture_output=True, text=True, timeout=timeout)
            return r.stdout.strip(), r.returncode
        except subprocess.TimeoutExpired:
            print(f"  [WARN] Timeout na tentativa {attempt+1}/{retries}. Reiniciando ADB...")
            restart_adb(adb)
            time.sleep(3)
        except Exception as e:
            print(f"  [ERROR] {e}")
            if attempt < retries - 1:
                restart_adb(adb)
                time.sleep(2)
    return None, -1

def restart_adb(adb):
    try:
        subprocess.run([adb, "kill-server"], capture_output=True, timeout=5)
        time.sleep(2)
        subprocess.run([adb, "start-server"], capture_output=True, timeout=10)
        time.sleep(3)
    except:
        pass

def check_device(adb):
    out, rc = run_adb(adb, ["devices"])
    if out is None:
        return False
    lines = out.strip().split("\n")
    devices = [l for l in lines[1:] if l.strip() and "device" in l]
    return len(devices) > 0

def check_app_running(adb):
    out, rc = run_adb(adb, ["shell", f"pidof {PACKAGE}"])
    return out is not None and out.strip() != ""

def install(adb):
    print("=== Instalacao ADB Resiliente ===")

    if not os.path.exists(APK_PATH):
        print(f"ERRO: APK nao encontrado: {APK_PATH}")
        return False

    print("1. Verificando conexao ADB...")
    if not check_device(adb):
        print("  Nenhum dispositivo encontrado. Tentando reconexao...")
        restart_adb(adb)
        time.sleep(3)
        if not check_device(adb):
            print("  FALHA: Dispositivo nao encontrado apos reconexao.")
            print("  Verifique: cabo USB, depuracao USB, drivers ADB.")
            return False
    print("  Dispositivo conectado.")

    print("2. Desinstalando versao anterior...")
    out, rc = run_adb(adb, ["uninstall", PACKAGE])
    if rc == 0:
        print("  Desinstalado com sucesso.")
    else:
        print(f"  Aviso: {out or 'Ja nao instalado'}")

    print("3. Instalando novo APK...")
    out, rc = run_adb(adb, ["install", APK_PATH], timeout=60)
    if rc == 0:
        print("  APK instalado com sucesso!")
    else:
        print(f"  ERRO na instalacao: {out}")
        return False

    print("4. Iniciando app...")
    out, rc = run_adb(adb, ["shell", "am", "start", "-n", f"{PACKAGE}/.ui.library.SplashActivity"])
    if rc == 0:
        print("  App iniciado.")
    else:
        print(f"  Aviso: {out}")

    time.sleep(3)
    if check_app_running(adb):
        print("  App rodando (verificado por pidof).")
    else:
        print("  AVISO: App pode nao estar rodando. Verifique manualmente.")

    print("=== Instalacao concluida ===")
    return True

def status(adb):
    print("=== Status ADB ===")

    out, rc = run_adb(adb, ["devices"])
    if out is None:
        print("  ADB: Nao respondendo")
        return
    print(f"  ADB: OK")
    print(f"  Dispositivos:\n{out}")

    if check_app_running(adb):
        pid = run_adb(adb, ["shell", f"pidof {PACKAGE}"])[0]
        print(f"  {PACKAGE}: rodando (PID {pid})")
    else:
        print(f"  {PACKAGE}: nao esta rodando")

    out, _ = run_adb(adb, ["shell", "dumpsys", "package", PACKAGE])
    if out:
        for line in out.split("\n"):
            if "versionName" in line:
                print(f"  Versao: {line.strip()}")
                break

def logcat(adb):
    print("=== Logcat (ultimas 50 linhas) ===")
    try:
        subprocess.run([adb, "logcat", "-d", "-t", "50", "-s", "AndroidRuntime:E", "System.err:W"],
                       timeout=10)
    except:
        print("Timeout ao ler logcat")

def main():
    adb = find_adb()
    if not adb:
        print("ERRO: ADB nao encontrado. Verifique o Android SDK.")
        sys.exit(1)

    cmd = sys.argv[1] if len(sys.argv) > 1 else "status"
    if cmd == "install":
        ok = install(adb)
        sys.exit(0 if ok else 1)
    elif cmd == "status":
        status(adb)
    elif cmd == "logcat":
        logcat(adb)
    elif cmd == "restart":
        print("Reiniciando ADB...")
        restart_adb(adb)
        print("Pronto.")
    else:
        print(f"Uso: {sys.argv[0]} [install|status|logcat|restart]")

if __name__ == "__main__":
    main()
