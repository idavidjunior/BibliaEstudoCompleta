package com.biblia.estudo.ui.crossref;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.biblia.estudo.R;
import com.biblia.estudo.app.BibliaApplication;
import com.biblia.estudo.data.HighlightDao;
import com.biblia.estudo.data.ReferenceEdgeDao;
import com.biblia.estudo.data.ReferenceMapDao;
import com.biblia.estudo.data.ReferenceNodeDao;
import com.biblia.estudo.model.ReferenceEdge;
import com.biblia.estudo.model.ReferenceMap;
import com.biblia.estudo.model.ReferenceNode;
import com.biblia.estudo.ui.bible.BibleReaderActivity;
import com.biblia.estudo.utils.NavigationHelper;

import java.util.ArrayList;
import java.util.List;

public class ReferenceMapActivity extends Activity {

    private ReferenceMapView mapView;
    private ReferenceMapDao mapDao;
    private ReferenceNodeDao nodeDao;
    private ReferenceEdgeDao edgeDao;
    private ReferenceMap currentMap;
    private TextView mapTitleView;
    private TextView mapStatsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BibliaApplication.getThemeManager().applyTheme(this);
        setContentView(R.layout.activity_reference_map);

        // Set toolbar as action bar for menu
        android.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);
        if (getActionBar() != null) {
            getActionBar().setDisplayShowTitleEnabled(false);
        }

        SQLiteDatabase db = BibliaApplication.getDatabaseManager().getReferenceMapDatabase();
        mapDao = new ReferenceMapDao(db);
        nodeDao = new ReferenceNodeDao(db);
        edgeDao = new ReferenceEdgeDao(db);

        mapView = findViewById(R.id.referenceMapView);
        mapTitleView = findViewById(R.id.mapTitle);
        mapStatsView = findViewById(R.id.mapStats);

        NavigationHelper.setupBackButton(this);
        NavigationHelper.setupBottomNav(this, R.id.navMore);

        mapView.setOnNodeClickListener(this::onNodeClick);
        mapView.setOnNodeLongClickListener(this::onNodeLongClick);
        mapView.setOnMapChangeListener(this::onMapChanged);

        long mapId = getIntent().getLongExtra("map_id", -1);
        if (mapId > 0) {
            loadMap(mapId);
        } else {
            showMapSelector();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reference_map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new_map) {
            showCreateMapDialog();
            return true;
        } else if (id == R.id.action_switch_map) {
            showMapSelector();
            return true;
        } else if (id == R.id.action_fit_screen) {
            mapView.fitToScreen();
            return true;
        } else if (id == R.id.action_add_node) {
            showAddNodeDialog();
            return true;
        } else if (id == R.id.action_export_map) {
            exportMap();
            return true;
        } else if (id == R.id.action_delete_map) {
            confirmDeleteMap();
            return true;
        } else if (id == R.id.action_reset_positions) {
            mapView.resetPositions();
            Toast.makeText(this, "Posições reorganizadas", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadMap(long mapId) {
        currentMap = mapDao.getById(mapId);
        if (currentMap == null) {
            Toast.makeText(this, "Mapa não encontrado", Toast.LENGTH_SHORT).show();
            showMapSelector();
            return;
        }

        List<ReferenceNode> nodes = nodeDao.getByMap(mapId);
        List<ReferenceEdge> edges = edgeDao.getByMap(mapId);
        mapView.setData(nodes, edges);

        mapTitleView.setText(currentMap.getTitle());
        updateStats();
        mapView.fitToScreen();
    }

    private void updateStats() {
        if (currentMap != null && mapStatsView != null) {
            int nodeCount = currentMap.getNodeCount();
            int edgeCount = currentMap.getEdgeCount();
            mapStatsView.setText(nodeCount + " nós, " + edgeCount + " conexões");
        }
    }

    private void onMapChanged() {
        updateStats();
        persistAllChanges();
    }

    private void persistAllChanges() {
        if (currentMap == null) return;
        
        // Persist node positions, colors, notes, expanded state
        List<ReferenceNode> nodes = nodeDao.getByMap(currentMap.getId());
        for (ReferenceNode node : nodes) {
            nodeDao.update(node);
        }
        
        // Persist edges (in case any were modified)
        List<ReferenceEdge> edges = edgeDao.getByMap(currentMap.getId());
        for (ReferenceEdge edge : edges) {
            edgeDao.update(edge);
        }
        
        // Update map counts
        int nodeCount = nodeDao.getCountByMap(currentMap.getId());
        int edgeCount = edgeDao.getCountByMap(currentMap.getId());
        mapDao.updateCounts(currentMap.getId(), nodeCount, edgeCount);
    }

    private void showMapSelector() {
        List<ReferenceMap> maps = mapDao.getAll();
        if (maps.isEmpty()) {
            showCreateMapDialog();
            return;
        }

        String[] options = new String[maps.size() + 1];
        options[0] = "+ Criar novo mapa";
        for (int i = 0; i < maps.size(); i++) {
            ReferenceMap m = maps.get(i);
            options[i + 1] = m.getTitle() + " (" + m.getNodeCount() + " nós)";
        }

        new AlertDialog.Builder(this)
                .setTitle("Referências Cruzadas")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showCreateMapDialog();
                    } else {
                        loadMap(maps.get(which - 1).getId());
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> finish())
                .show();
    }

    private void showCreateMapDialog() {
        EditText input = new EditText(this);
        input.setHint("Nome do mapa (ex: Fé, Salvação, Espírito Santo)");
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
                .setTitle("Criar novo mapa")
                .setView(input)
                .setPositiveButton("Criar", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, "Digite um nome", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ReferenceMap map = new ReferenceMap();
                    map.setTitle(title);
                    map.setDescription("");
                    long mapId = mapDao.insert(map);
                    loadMap(mapId);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showAddNodeDialog() {
        if (currentMap == null) return;

        SQLiteDatabase bibleDb = BibliaApplication.getDatabaseManager().getBibleDatabase();
        com.biblia.estudo.data.BookDao bookDao = new com.biblia.estudo.data.BookDao(bibleDb);
        List<com.biblia.estudo.model.Book> books = bookDao.getAll();

        String[] bookNames = new String[books.size()];
        for (int i = 0; i < books.size(); i++) {
            bookNames[i] = books.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Adicionar versículo ao mapa");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_node, null);
        android.widget.Spinner bookSpinner = dialogView.findViewById(R.id.nodeBookSpinner);
        EditText chapterInput = dialogView.findViewById(R.id.nodeChapterInput);
        EditText verseInput = dialogView.findViewById(R.id.nodeVerseInput);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bookNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bookSpinner.setAdapter(adapter);

        builder.setView(dialogView);
        builder.setPositiveButton("Adicionar", (dialog, which) -> {
            int bookIndex = bookSpinner.getSelectedItemPosition();
            com.biblia.estudo.model.Book selectedBook = books.get(bookIndex);

            String chapterStr = chapterInput.getText().toString().trim();
            String verseStr = verseInput.getText().toString().trim();

            if (chapterStr.isEmpty() || verseStr.isEmpty()) {
                Toast.makeText(this, "Preencha capítulo e versículo", Toast.LENGTH_SHORT).show();
                return;
            }

            int chapter = Integer.parseInt(chapterStr);
            int verse = Integer.parseInt(verseStr);

            // Fetch verse text
            com.biblia.estudo.data.VerseDao verseDao = new com.biblia.estudo.data.VerseDao(bibleDb);
            List<com.biblia.estudo.model.Verse> verses = verseDao.getVersesRange(selectedBook.getId(), chapter, verse, verse,
                com.biblia.estudo.app.BibliaApplication.getActiveTranslation());
            String verseText = verses.isEmpty() ? "" : verses.get(0).getText();

            // Check if already exists
            ReferenceNode existing = nodeDao.findByReference(currentMap.getId(), selectedBook.getId(), chapter, verse);
            if (existing != null) {
                Toast.makeText(this, "Este versículo já existe no mapa", Toast.LENGTH_SHORT).show();
                mapView.centerOnNode(existing);
                return;
            }

            ReferenceNode node = new ReferenceNode();
            node.setMapId(currentMap.getId());
            node.setBookId(selectedBook.getId());
            node.setBookName(selectedBook.getName());
            node.setChapter(chapter);
            node.setVerse(verse);
            node.setVerseText(verseText);
            node.setTranslation("ARA");
            node.setPositionX(0);
            node.setPositionY(0);
            nodeDao.insert(node);

            mapView.addNode(node);
            Toast.makeText(this, "Versículo adicionado", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void onNodeClick(ReferenceNode node) {
        openInBibleReader(node);
    }

    private void showNodeActions(ReferenceNode node) {
        String[] actions = {
                "Abrir versículo na Bíblia",
                "Ler contexto",
                "Editar anotação",
                "Alterar cor",
                "Criar conexão",
                "Adicionar referência relacionada",
                "Remover do mapa"
        };

        new AlertDialog.Builder(this)
                .setTitle(node.getReference())
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0: openInBibleReader(node); break;
                        case 1: openContext(node); break;
                        case 2: editNote(node); break;
                        case 3: changeColor(node); break;
                        case 4: createConnection(node); break;
                        case 5: addRelatedReference(node); break;
                        case 6: removeNode(node); break;
                    }
                })
                .show();
    }

    private boolean onNodeLongClick(ReferenceNode node) {
        showNodeActions(node);
        return true;
    }

    private void openInBibleReader(ReferenceNode node) {
        SQLiteDatabase bibleDb = BibliaApplication.getDatabaseManager().getBibleDatabase();
        com.biblia.estudo.data.BookDao bookDao = new com.biblia.estudo.data.BookDao(bibleDb);
        com.biblia.estudo.model.Book book = bookDao.getById(node.getBookId());
        int chapterCount = book != null ? book.getChapterCount() : 50;

        Intent intent = new Intent(this, BibleReaderActivity.class);
        intent.putExtra("book_id", node.getBookId());
        intent.putExtra("book_name", node.getBookName());
        intent.putExtra("chapter_count", chapterCount);
        intent.putExtra("chapter", node.getChapter());
        intent.putExtra("verse", node.getVerse());
        startActivity(intent);
    }

    private void openContext(ReferenceNode node) {
        SQLiteDatabase bibleDb = BibliaApplication.getDatabaseManager().getBibleDatabase();
        com.biblia.estudo.data.VerseDao verseDao = new com.biblia.estudo.data.VerseDao(bibleDb);
        List<com.biblia.estudo.model.Verse> verses = verseDao.getChapter(node.getBookId(), node.getChapter(),
                com.biblia.estudo.app.BibliaApplication.getActiveTranslation());

        StringBuilder sb = new StringBuilder();
        sb.append(node.getBookName()).append(" ").append(node.getChapter()).append("\n\n");
        for (com.biblia.estudo.model.Verse v : verses) {
            sb.append(v.getVerseNumber()).append(". ").append(v.getText()).append("\n\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Contexto - " + node.getReference())
                .setMessage(sb.toString())
                .setPositiveButton("Fechar", null)
                .show();
    }

    private void editNote(ReferenceNode node) {
        EditText input = new EditText(this);
        input.setHint("Anotação para este versículo...");
        if (node.getNote() != null) input.setText(node.getNote());
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
                .setTitle("Anotação - " + node.getReference())
                .setView(input)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    node.setNote(text.isEmpty() ? null : text);
                    nodeDao.update(node);
                    Toast.makeText(this, "Anotação salva", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void changeColor(ReferenceNode node) {
        final String[] colors = {"#FFF9C4", "#FFCDD2", "#C8E6C9", "#BBDEFB", "#E1BEE7", "#FFE0B2"};
        final String[] labels = {"Amarelo", "Vermelho", "Verde", "Azul", "Roxo", "Laranja"};

        new AlertDialog.Builder(this)
                .setTitle("Cor do destaque - " + node.getReference())
                .setItems(labels, (dialog, which) -> {
                    String color = colors[which];
                    if (node.getColor() != null && node.getColor().equals(color)) {
                        // Same color - remove
                        node.setColor(null);
                    } else {
                        node.setColor(color);
                    }
                    nodeDao.update(node);
                    mapView.updateNode(node);

                    // Also update in Bible highlights for consistency
                    updateBibleHighlight(node, color);

                    Toast.makeText(this, "Cor atualizada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Remover cor", (dialog, which) -> {
                    node.setColor(null);
                    nodeDao.update(node);
                    mapView.updateNode(node);
                    updateBibleHighlight(node, null);
                    Toast.makeText(this, "Cor removida", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void updateBibleHighlight(ReferenceNode node, String color) {
        SQLiteDatabase bibleDb = BibliaApplication.getDatabaseManager().getBibleDatabase();
        HighlightDao highlightDao = new HighlightDao(bibleDb);
        if (color != null) {
            com.biblia.estudo.model.Highlight h = new com.biblia.estudo.model.Highlight();
            h.setBookId(node.getBookId());
            h.setChapter(node.getChapter());
            h.setVerseStart(node.getVerse());
            h.setVerseEnd(node.getVerse());
            h.setColor(color);
            highlightDao.insert(h);
        } else {
            highlightDao.deleteByVerse(node.getBookId(), node.getChapter(), node.getVerse());
        }
    }

    private void createConnection(ReferenceNode sourceNode) {
        List<ReferenceNode> targetNodes = new ArrayList<>();
        for (ReferenceNode n : nodeDao.getByMap(currentMap.getId())) {
            if (n.getId() != sourceNode.getId()) {
                targetNodes.add(n);
            }
        }

        if (targetNodes.isEmpty()) {
            Toast.makeText(this, "Adicione mais versículos para criar conexões", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = new String[targetNodes.size()];
        for (int i = 0; i < targetNodes.size(); i++) {
            options[i] = targetNodes.get(i).getReference();
        }

        new AlertDialog.Builder(this)
                .setTitle("Conectar " + sourceNode.getReference() + " a:")
                .setItems(options, (dialog, which) -> {
                    ReferenceNode targetNode = targetNodes.get(which);
                    showRelationTypeDialog(sourceNode, targetNode);
                })
                .show();
    }

    private void showRelationTypeDialog(ReferenceNode source, ReferenceNode target) {
        final String[] types = {
                ReferenceEdge.TYPE_TEMATICA,
                ReferenceEdge.TYPE_PROFECIA,
                ReferenceEdge.TYPE_CUMPRIMENTO,
                ReferenceEdge.TYPE_PARALELO,
                ReferenceEdge.TYPE_EXPLICACAO,
                ReferenceEdge.TYPE_COMPLEMENTO,
                ReferenceEdge.TYPE_CONTRASTE,
                ReferenceEdge.TYPE_CONTEXTO,
                ReferenceEdge.TYPE_DOUTRINA,
                ReferenceEdge.TYPE_PERSONAGEM,
                ReferenceEdge.TYPE_EVENTO,
                ReferenceEdge.TYPE_PALAVRA_CHAVE,
                ReferenceEdge.TYPE_PERSONALIZADA
        };

        final String[] labels = {
                "Relação temática",
                "Profecia",
                "Cumprimento",
                "Paralelo",
                "Explicação",
                "Complemento",
                "Contraste",
                "Contexto",
                "Doutrina",
                "Personagem",
                "Evento",
                "Palavra-chave",
                "Personalizada"
        };

        new AlertDialog.Builder(this)
                .setTitle("Tipo de relação")
                .setSingleChoiceItems(labels, 0, (dialog, which) -> {
                    String type = types[which];
                    ReferenceEdge edge = new ReferenceEdge();
                    edge.setMapId(currentMap.getId());
                    edge.setSourceNodeId(source.getId());
                    edge.setTargetNodeId(target.getId());
                    edge.setRelationType(type);
                    edgeDao.insert(edge);
                    mapView.addEdge(edge);
                    Toast.makeText(this, "Conexão criada: " + labels[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .show();
    }

    private void addRelatedReference(ReferenceNode node) {
        // Could open search or show suggestions
        Toast.makeText(this, "Use a pesquisa para encontrar referências relacionadas", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, com.biblia.estudo.ui.search.SearchActivity.class));
    }

    private void removeNode(ReferenceNode node) {
        new AlertDialog.Builder(this)
                .setTitle("Remover do mapa")
                .setMessage("Remover " + node.getReference() + " e suas conexões?")
                .setPositiveButton("Remover", (dialog, which) -> {
                    // Remove edges connected to this node
                    List<ReferenceEdge> edges = edgeDao.getByMap(currentMap.getId());
                    for (ReferenceEdge edge : edges) {
                        if (edge.getSourceNodeId() == node.getId() || edge.getTargetNodeId() == node.getId()) {
                            edgeDao.delete(edge.getId());
                        }
                    }
                    nodeDao.delete(node.getId());
                    mapView.removeNode(node.getId());
                    Toast.makeText(this, "Removido", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void exportMap() {
        // TODO: Implement export functionality
        Toast.makeText(this, "Exportação em desenvolvimento", Toast.LENGTH_SHORT).show();
    }

    private void confirmDeleteMap() {
        if (currentMap == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Excluir mapa")
                .setMessage("Excluir '" + currentMap.getTitle() + "' e todos os seus dados?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    mapDao.delete(currentMap.getId());
                    Toast.makeText(this, "Mapa excluído", Toast.LENGTH_SHORT).show();
                    showMapSelector();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}