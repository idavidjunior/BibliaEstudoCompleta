# Arquitetura do BibliaEstudoCompleta

## Visão geral

O aplicativo é organizado em camadas, com o código dentro de
`src/com/biblia/estudo`.

## Camadas

### `app`

`BibliaApplication` cria os bancos no primeiro uso e `ThemeManager` controla
o tema claro e escuro.

### `data`

Cada assunto tem seu próprio arquivo SQLite e seu próprio helper:

| Banco | Helper |
|-------|--------|
| Bíblia principal | `BibleDatabaseHelper` |
| Comentários | `CommentaryDatabaseHelper` |
| Dicionário | `DictionaryDatabaseHelper` |
| Referências cruzadas | `CrossReferenceDatabaseHelper` |
| Mapa de referências | `ReferenceMapDatabaseHelper` |
| Índice de tópicos | `TopicIndexDatabaseHelper` |

Os DAOs ficam na mesma camada: `VerseDao`, `BookDao`, `TranslationDao`,
`CommentaryDao`, `DictionaryEntry` junto de `FavoriteDao`, `NoteDao`,
`HighlightDao`, `ReadingProgressDao`, `GroupDao`, `ResourceFolderDao`,
`UserResourceDao`, `ReferenceNodeDao`, `ReferenceEdgeDao` e
`ReferenceMapDao`.

`DataInitializer` semeia os bancos e `DatabaseManager` concentra o acesso.

### `model`

Modelos de domínio: `Verse`, `Book`, `Translation`, `Commentary`,
`CrossReference`, `DictionaryEntry`, `Favorite`, `Group`, `Highlight`,
`ReadingPlan`, `ReadingProgress`, `UserNote`, `UserResource`, `ResourceFolder`,
`BibleMap`, `BiblicalFigure`, `TimelineEvent`, `ReferenceNode`, `ReferenceEdge`
e `ReferenceMap`.

### `ui`

Uma pasta por assunto da interface:

`about`, `apocrypha`, `bible`, `crossref`, `dictionary`, `favorites`,
`highlights`, `library`, `maps`, `notes`, `readingplan`, `resources`, `search`,
`settings`, `study` e `timeline`.

### `utils`

`SearchEngine` e `SearchUtils` cuidam da busca. `BackupManager` faz cópias de
segurança. `ShareUtils` compartilha versículos. `NavigationHelper` controla a
navegação entre telas. `StorageOrigin` identifica a origem dos recursos.

### `audio`

`AudioService` narra os versículos.

## Motor de transições

As transições de página usam OpenGL ES 2.0. A projeção correta é ortográfica,
com `orthoM(-1, 1, -1, 1, 2, 8)`, porque a página tem a mesma proporção da
tela. A multiplicação de `pos.x` pela proporção antiga quebrava o
preenchimento da tela.

## Dados

Os bancos prontos ficam em `assets/databases/`. O `scripts/inspect_dbs.py`
inspeciona as tabelas e o `scripts/inspect_verses_schema.py` mostra o esquema
da tabela de versículos.
