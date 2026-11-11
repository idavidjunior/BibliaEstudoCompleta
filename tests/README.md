# Testes do BibliaEstudoCompleta

O projeto não usa framework de teste de unidade. A verificação é feita por
script Python padrão, sem dependências externas.

## Executar

```powershell
python tests/verificar_integridade.py
```

O script sai com código 1 quando alguma verificação falha.

## O que é verificado

- Presença das camadas de código: `app`, `audio`, `data`, `model`, `ui` e
  `utils`.
- Quantidade mínima de classes Java e de arquivos Kotlin.
- Presença dos bancos em `assets/databases/`.
- Tabelas `books` e `verses` no banco da Bíblia, com a contagem de versículos.
- Presença dos recursos de layout, do manifesto e do script de build.
- Presença dos artefatos da engenharia reversa em `extracao/`.

## Outros scripts de verificação do projeto

O próprio projeto traz scripts úteis dentro de `scripts/`:

```powershell
python scripts/inspect_dbs.py
python scripts/inspect_verses_schema.py
python scripts/test_search.py
python scripts/test_rebuild.py
```
