# ADR 0001: Separar cada assunto em seu próprio banco

## Status

Aceito.

## Contexto

A Bíblia, os comentários, o dicionário e as referências cruzadas crescem em
ritmos diferentes. Juntos em um único arquivo, qualquer mudança no dicionário
obrigaria a reescrever o arquivo maior da Bíblia, e o aparelho pagaria esse
custo toda vez que abrisse o aplicativo.

## Decisão

Um arquivo SQLite por assunto, cada um com seu helper e seu DAO:

`biblia_estudo.db`, `comentarios.db`, `dicionario.db`, `indices.db`,
`referencias.db`, `referencias_cruzadas.db`.

## Consequências

- Uma atualização em um assunto não mexe nos outros.
- O `DataInitializer` semeia cada banco na primeira execução.
- `scripts/inspect_dbs.py` inspeciona os bancos de forma independente.
