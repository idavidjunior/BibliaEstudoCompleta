# BibliaEstudoCompleta

Aplicativo Android de estudo bíblico com traduções, comentários, dicionário,
referências cruzadas, notas do usuário e biblioteca de recursos.

## O que o aplicativo faz

- Leitura da Bíblia em duas traduções: Almeida 1911 e Almeida Revisada.
- Comentários, dicionário e tápicos em bancos de dados separados.
- Referências cruzadas entre versículos.
- Marcadores, notas, destaques e planos de leitura.
- Biblioteca de recursos do usuário, com importação de pastas.
- Mapas bíblicos, cronologia e figuras bíblicas.

## Stack

- Java e Kotlin, Android puro com script de build próprio.
- SQLite para os dados, com um arquivo por assunto.
- Recursos em XML e transições com OpenGL ES 2.0.

## Estrutura

| Pasta | Responsabilidade |
|-------|------------------|
| `src/com/biblia/estudo/app` | Aplicação, tema e inicialização |
| `src/com/biblia/estudo/audio` | Narração por áudio dos versículos |
| `src/com/biblia/estudo/data` | Acesso aos bancos de dados e DAOs |
| `src/com/biblia/estudo/model` | Modelos de domínio |
| `src/com/biblia/estudo/ui` | Telas, uma pasta por assunto |
| `src/com/biblia/estudo/utils` | Busca, compartilhamento, backup e navegação |
| `assets/databases` | Bancos SQLite prontos para o aplicativo |
| `res` | Layouts, drawables, animações e valores |
| `scripts` | Scripts de análise, importação e verificação |
| `extracao` | Artefatos da engenharia reversa: APK, código-fonte e interface extraída |
| `docs` | Documentação, inventário e licença |

## Build

```powershell
.\build.ps1
```

## Verificação

```powershell
python tests/verificar_integridade.py
```

O script confere a presença do código, dos bancos de dados, dos recursos e
dos scripts de análise. Ele sai com código 1 quando algo falta.

## Licença do conteúdo bíblico

A tradução Almeida 1911 é de domínio público. A Almeida Revisada é licenciada
pela SBB e não pode ser redistribuída. O detalhe está em `docs/LICENCA.md`.

## Documentação

- Arquitetura: `docs/architecture/arquitetura.md`
- Diagramas: `docs/diagrams/`
- Decisões: `docs/adr/`
- Inventário da extração: `docs/INVENTARIO.md`
