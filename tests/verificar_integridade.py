"""Verificacao de integridade do projeto BibliaEstudoCompleta.

Confere que o codigo, os bancos, os recursos e os scripts existem.
Uso: python tests/verificar_integridade.py
"""

import sqlite3
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
FALHAS = []


def checar(condicao, mensagem):
    if condicao:
        print(f"[OK] {mensagem}")
    else:
        print(f"[FALHA] {mensagem}")
        FALHAS.append(mensagem)


def main():
    codigo = RAIZ / "src" / "com" / "biblia" / "estudo"
    checar(codigo.is_dir(), "pasta de codigo existe: src/com/biblia/estudo")

    for camada in ["app", "audio", "data", "model", "ui", "utils"]:
        checar((codigo / camada).is_dir(), f"camada existe: {camada}")

    classes = list(codigo.rglob("*.java"))
    checar(len(classes) >= 40, f"projeto tem {len(classes)} classes Java")
    kotlin = list((RAIZ / "src" / "main" / "kotlin").rglob("*.kt"))
    checar(len(kotlin) >= 1, f"projeto tem {len(kotlin)} arquivos Kotlin")

    bancos = list((RAIZ / "assets" / "databases").glob("*.db"))
    checar(len(bancos) >= 5, f"projeto tem {len(bancos)} bancos de dados")

    biblia = RAIZ / "assets" / "databases" / "biblia_estudo.db"
    if biblia.is_file():
        conexao = sqlite3.connect(f"file:{biblia}?mode=ro", uri=True)
        try:
            tabelas = {
                linha[0]
                for linha in conexao.execute(
                    "SELECT name FROM sqlite_master WHERE type='table'"
                )
            }
            checar("books" in tabelas, "banco da biblia tem a tabela books")
            checar("verses" in tabelas, "banco da biblia tem a tabela verses")
            if "verses" in tabelas:
                total = conexao.execute("SELECT COUNT(*) FROM verses").fetchone()[0]
                checar(total > 1000, f"banco da biblia tem {total} versiculos")
        finally:
            conexao.close()

    checar((RAIZ / "res" / "layout").is_dir(), "recursos de layout existem")
    checar((RAIZ / "AndroidManifest.xml").is_file(), "AndroidManifest.xml existe")
    checar((RAIZ / "build.ps1").is_file(), "build.ps1 existe")

    extracao = RAIZ / "extracao"
    checar((extracao / "apk_original" / "BibliaEstudoCompleta.apk").is_file(),
           "APK original preservado em extracao/apk_original")
    checar((extracao / "codigo-fonte" / "CODIGO_FONTE_BIBLIA_ALM1911.json").is_file(),
           "codigo-fonte ALM1911 preservado em extracao/codigo-fonte")

    print()
    if FALHAS:
        print(f"RESULTADO: {len(FALHAS)} verificacao(oes) falharam")
        return 1
    print("RESULTADO: todas as verificacoes passaram")
    return 0


if __name__ == "__main__":
    sys.exit(main())
