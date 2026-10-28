package com.biblia.estudo.ui.search;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.biblia.estudo.R;
import com.biblia.estudo.app.BibliaApplication;
import com.biblia.estudo.data.HighlightDao;
import com.biblia.estudo.data.ReferenceEdgeDao;
import com.biblia.estudo.data.ReferenceMapDao;
import com.biblia.estudo.data.ReferenceNodeDao;
import com.biblia.estudo.data.TranslationDao;
import com.biblia.estudo.model.ReferenceEdge;
import com.biblia.estudo.model.ReferenceMap;
import com.biblia.estudo.model.ReferenceNode;
import com.biblia.estudo.model.Translation;
import com.biblia.estudo.ui.bible.BibleReaderActivity;
import com.biblia.estudo.utils.NavigationHelper;
import com.biblia.estudo.utils.SearchEngine;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends Activity {

    private EditText searchInput;
    private ListView resultsList;
    private TextView noResults;
    private TextView resultsCount;
    private Spinner translationSpinner;
    private List<Translation> translations;
    private SearchEngine searchEngine;
    private SearchResultsAdapter adapter;
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private boolean translationInitializing = true;

    private int searchMode = 0;
    private RadioButton searchWord, searchPhrase, searchBook, searchTopic;
    private HighlightDao highlightDao;
    private ActionMode actionMode;
    private Cursor lastSearchCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BibliaApplication.getThemeManager().applyTheme(this);
        setContentView(R.layout.activity_search);

        searchEngine = new SearchEngine(BibliaApplication.getDatabaseManager());
        SQLiteDatabase db = BibliaApplication.getDatabaseManager().getBibleDatabase();
        highlightDao = new HighlightDao(db);

        searchInput = findViewById(R.id.searchInput);
        resultsList = findViewById(R.id.resultsList);
        noResults = findViewById(R.id.noResults);
        resultsCount = findViewById(R.id.resultsCount);
        translationSpinner = findViewById(R.id.translationSpinner);

        NavigationHelper.setupBottomNav(this, R.id.navSearch);

        setupTranslationSpinner();
        setupSearchInput();
        setupResultsList();
    }

    private void setupTranslationSpinner() {
        translations = new TranslationDao(BibliaApplication.getDatabaseManager().getBibleDatabase()).getAll();
        if (translations == null || translations.isEmpty()) {
            translations = new ArrayList<>();
            Translation t = new Translation();
            t.setCode(BibliaApplication.getActiveTranslation());
            t.setName(TranslationDao.DEFAULT_TRANSLATION);
            translations.add(t);
        }
        List<String> names = new ArrayList<>();
        for (Translation t : translations) {
            names.add(t.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        translationSpinner.setAdapter(adapter);

        String active = BibliaApplication.getActiveTranslation();
        for (int i = 0; i < translations.size(); i++) {
            if (translations.get(i).getCode().equals(active)) {
                translationSpinner.setSelection(i);
                break;
            }
        }
        translationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (translationInitializing) return;
                Translation t = translations.get(position);
                if (t == null) return;
                BibliaApplication.setActiveTranslation(t.getCode());
                searchEngine.setTranslationId(t.getCode());
                String query = searchInput.getText().toString().trim();
                if (query.length() >= 2) performSearch(query);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        translationInitializing = false;
    }

    private void setupTabs() {
        View.OnClickListener l = v -> {
            RadioButton[] tabs = {searchWord, searchPhrase, searchBook, searchTopic};
            for (int i = 0; i < tabs.length; i++) {
                boolean selected = tabs[i] == v;
                tabs[i].setChecked(selected);
                if (selected) {
                    searchMode = i;
                    tabs[i].setTextColor(getResources().getColor(R.color.accent));
                } else {
                    tabs[i].setTextColor(getResources().getColor(R.color.text_secondary));
                }
            }
            String q = searchInput.getText().toString().trim();
            if (q.length() >= 2) performSearch(q);
        };
        searchWord.setOnClickListener(l);
        searchPhrase.setOnClickListener(l);
        searchBook.setOnClickListener(l);
        searchTopic.setOnClickListener(l);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String current = BibliaApplication.getActiveTranslation();
        if (searchEngine != null) {
            searchEngine.setTranslationId(current);
        }
        if (translationSpinner != null && translations != null) {
            for (int i = 0; i < translations.size(); i++) {
                if (translations.get(i).getCode().equals(current)) {
                    if (translationSpinner.getSelectedItemPosition() != i) {
                        translationSpinner.setSelection(i);
                    }
                    break;
                }
            }
        }
        String query = searchInput.getText().toString().trim();
        if (query.length() >= 2) performSearch(query);
    }

    private void setupSearchInput() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.length() < 2) {
                    resultsList.setVisibility(View.GONE);
                    noResults.setVisibility(View.GONE);
                    updateResultsCount(0);
                    return;
                }
                searchRunnable = () -> performSearch(query);
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });
    }

    private void setupResultsList() {
        resultsList.setOnItemClickListener((parent, view, position, id) -> {
            if (adapter != null && adapter.isSelectionMode()) {
                adapter.toggleSelection(id);
                updateActionModeTitle();
            } else {
                openVerseFromCursor(position);
            }
        });

        resultsList.setOnItemLongClickListener((parent, view, position, id) -> {
            if (adapter != null && !adapter.isSelectionMode()) {
                startActionMode();
                adapter.toggleSelection(id);
                updateActionModeTitle();
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        if (lastSearchCursor != null) {
            lastSearchCursor.close();
            lastSearchCursor = null;
        }

        Cursor cursor = null;
        switch (searchMode) {
            case 0:
                cursor = searchEngine.searchByWordCursor(query);
                break;
            case 1:
                cursor = searchEngine.searchByPhraseCursor(query);
                break;
            case 2:
                cursor = searchEngine.searchByBookCursor(query);
                break;
            case 3:
                cursor = searchEngine.searchByTopic(query);
                break;
        }

        if (cursor != null && cursor.getCount() > 0) {
            lastSearchCursor = cursor;
            noResults.setVisibility(View.GONE);
            resultsList.setVisibility(View.VISIBLE);
            adapter = new SearchResultsAdapter(this, cursor, highlightDao);
            resultsList.setAdapter(adapter);
            updateResultsCount(cursor.getCount());

            // Set chapter context for color indicators
            if (cursor.moveToFirst()) {
                long bookId = cursor.getLong(cursor.getColumnIndexOrThrow("book_id"));
                int chapter = cursor.getInt(cursor.getColumnIndexOrThrow("chapter"));
                adapter.setCurrentChapterContext(bookId, chapter);
            }
            cursor.moveToFirst();

            resultsList.setOnItemClickListener((parent, view, position, id) -> {
                if (adapter != null && adapter.isSelectionMode()) {
                    adapter.toggleSelection(id);
                    updateActionModeTitle();
                } else {
                    openVerseFromCursor(position);
                }
            });
        } else {
            noResults.setVisibility(View.VISIBLE);
            resultsList.setVisibility(View.GONE);
            updateResultsCount(0);
            if (cursor != null) cursor.close();
        }
    }

    private void openVerseFromCursor(int position) {
        if (adapter == null || lastSearchCursor == null) return;
        Cursor c = (Cursor) adapter.getItem(position);
        if (c == null) return;

        long bookId = c.getLong(c.getColumnIndexOrThrow("book_id"));
        int chapter = c.getInt(c.getColumnIndexOrThrow("chapter"));
        int verse = c.getInt(c.getColumnIndexOrThrow("verse_number"));
        String bookName = c.getString(c.getColumnIndexOrThrow("book_name"));
        int chapterCount = 1;
        int colIdx = c.getColumnIndex("chapter_count");
        if (colIdx >= 0) {
            chapterCount = c.getInt(colIdx);
        }

        Intent intent = new Intent(SearchActivity.this, BibleReaderActivity.class);
        intent.putExtra("book_id", bookId);
        intent.putExtra("book_name", bookName);
        intent.putExtra("chapter_count", chapterCount);
        intent.putExtra("chapter", chapter);
        intent.putExtra("verse", verse);
        startActivity(intent);
    }

    private void updateResultsCount(int count) {
        if (resultsCount != null) {
            resultsCount.setText(count + " " + getResources().getQuantityString(R.plurals.search_results_count, count, count));
        }
    }

    private void startActionMode() {
        if (actionMode == null) {
            actionMode = startActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    mode.getMenuInflater().inflate(R.menu.search_context_menu, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    if (item.getItemId() == R.id.action_select_all) {
                        selectAllResults();
                        return true;
                    } else if (item.getItemId() == R.id.action_color) {
                        showColorPickerForSelected();
                        return true;
                    } else if (item.getItemId() == R.id.action_add_to_cross_refs) {
                        addSelectedToCrossReferences();
                        return true;
                    } else if (item.getItemId() == R.id.action_clear_selection) {
                        clearSelection();
                        return true;
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    if (adapter != null) {
                        adapter.setSelectionMode(false);
                        adapter.clearSelection();
                    }
                    actionMode = null;
                }
            });
        }
        if (adapter != null) {
            adapter.setSelectionMode(true);
        }
    }

    private void selectAllResults() {
        if (lastSearchCursor == null || adapter == null) return;

        // Collect all IDs first to avoid moving the shared cursor
        java.util.List<Long> allIds = new java.util.ArrayList<>();
        int pos = lastSearchCursor.getPosition();
        lastSearchCursor.moveToPosition(-1);
        while (lastSearchCursor.moveToNext()) {
            long id = lastSearchCursor.getLong(lastSearchCursor.getColumnIndexOrThrow("_id"));
            allIds.add(id);
        }
        lastSearchCursor.moveToPosition(pos);

        // Add all at once, then notify once
        for (Long id : allIds) {
            adapter.getSelectedIds().add(id);
        }
        adapter.notifyDataSetChanged();
        updateActionModeTitle();
    }

    private void clearSelection() {
        if (adapter != null) {
            adapter.clearSelection();
        }
        updateActionModeTitle();
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private void updateActionModeTitle() {
        if (actionMode != null && adapter != null) {
            int count = adapter.getSelectedIds().size();
            actionMode.setTitle(count + " selecionado(s)");
        }
    }

    private void showColorPickerForSelected() {
        if (adapter == null || adapter.getSelectedIds().isEmpty()) return;

        final String[] colors = {"#FFF9C4", "#FFCDD2", "#C8E6C9", "#BBDEFB", "#E1BEE7", "#FFE0B2"};
        final String[] labels = {"Amarelo", "Vermelho", "Verde", "Azul", "Roxo", "Laranja"};

        new AlertDialog.Builder(this)
                .setTitle("Marcar versículos selecionados")
                .setItems(labels, (dialog, which) -> {
                    String color = colors[which];
                    applyColorToSelected(color);
                })
                .setNegativeButton("Remover cor", (dialog, which) -> {
                    applyColorToSelected(null);
                })
                .show();
    }

    private void applyColorToSelected(String color) {
        if (adapter == null || lastSearchCursor == null) return;

        // Build id -> (bookId, chapter, verse) map first
        java.util.Map<Long, int[]> verseData = new java.util.HashMap<>();
        int pos = lastSearchCursor.getPosition();
        lastSearchCursor.moveToPosition(-1);
        while (lastSearchCursor.moveToNext()) {
            long id = lastSearchCursor.getLong(lastSearchCursor.getColumnIndexOrThrow("_id"));
            long bookId = lastSearchCursor.getLong(lastSearchCursor.getColumnIndexOrThrow("book_id"));
            int chapter = lastSearchCursor.getInt(lastSearchCursor.getColumnIndexOrThrow("chapter"));
            int verse = lastSearchCursor.getInt(lastSearchCursor.getColumnIndexOrThrow("verse_number"));
            verseData.put(id, new int[]{(int) bookId, chapter, verse});
        }
        lastSearchCursor.moveToPosition(pos);

        int updated = 0;
        for (Long verseId : adapter.getSelectedIds()) {
            int[] data = verseData.get(verseId);
            if (data != null) {
                long bookId = data[0];
                int chapter = data[1];
                int verse = data[2];

                if (color != null) {
                    com.biblia.estudo.model.Highlight h = new com.biblia.estudo.model.Highlight();
                    h.setBookId(bookId);
                    h.setChapter(chapter);
                    h.setVerseStart(verse);
                    h.setVerseEnd(verse);
                    h.setColor(color);
                    highlightDao.insert(h);
                } else {
                    highlightDao.deleteByVerse(bookId, chapter, verse);
                }
                updated++;
            }
        }
        adapter.notifyDataSetChanged();
        Toast.makeText(this, updated + " versículo(s) atualizado(s)", Toast.LENGTH_SHORT).show();
    }

    private void addSelectedToCrossReferences() {
        if (adapter == null || adapter.getSelectedIds().isEmpty() || lastSearchCursor == null) return;

        // Build id -> VerseSelection map first
        java.util.Map<Long, VerseSelection> verseMap = new java.util.HashMap<>();
        int pos = lastSearchCursor.getPosition();
        lastSearchCursor.moveToPosition(-1);
        while (lastSearchCursor.moveToNext()) {
            long id = lastSearchCursor.getLong(lastSearchCursor.getColumnIndexOrThrow("_id"));
            if (adapter.getSelectedIds().contains(id)) {
                long bookId = lastSearchCursor.getLong(lastSearchCursor.getColumnIndexOrThrow("book_id"));
                String bookName = lastSearchCursor.getString(lastSearchCursor.getColumnIndexOrThrow("book_name"));
                int chapter = lastSearchCursor.getInt(lastSearchCursor.getColumnIndexOrThrow("chapter"));
                int verse = lastSearchCursor.getInt(lastSearchCursor.getColumnIndexOrThrow("verse_number"));
                String text = lastSearchCursor.getString(lastSearchCursor.getColumnIndexOrThrow("text"));
                verseMap.put(id, new VerseSelection(bookId, bookName, chapter, verse, text));
            }
        }
        lastSearchCursor.moveToPosition(pos);

        List<VerseSelection> selections = new ArrayList<>(verseMap.values());
        if (selections.isEmpty()) return;

        showMapSelectionDialog(selections);
    }

    private void showMapSelectionDialog(List<VerseSelection> selections) {
        SQLiteDatabase db = BibliaApplication.getDatabaseManager().getReferenceMapDatabase();
        ReferenceMapDao mapDao = new ReferenceMapDao(db);
        List<ReferenceMap> maps = mapDao.getAll();

        String[] options = new String[maps.size() + 1];
        options[0] = "+ Criar novo mapa";
        for (int i = 0; i < maps.size(); i++) {
            options[i + 1] = maps.get(i).getTitle();
        }

        new AlertDialog.Builder(this)
                .setTitle("Adicionar às Referências Cruzadas")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showCreateMapDialog(selections);
                    } else {
                        ReferenceMap selectedMap = maps.get(which - 1);
                        addVersesToMap(selectedMap.getId(), selections);
                    }
                })
                .show();
    }

    private void showCreateMapDialog(List<VerseSelection> selections) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Nome do mapa (ex: Fé, Salvação, Espírito Santo)");
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
                .setTitle("Criar novo mapa de referências")
                .setView(input)
                .setPositiveButton("Criar", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, "Digite um nome para o mapa", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SQLiteDatabase db = BibliaApplication.getDatabaseManager().getReferenceMapDatabase();
                    ReferenceMapDao mapDao = new ReferenceMapDao(db);
                    ReferenceMap map = new ReferenceMap();
                    map.setTitle(title);
                    map.setDescription("Criado a partir de pesquisa");
                    long mapId = mapDao.insert(map);
                    addVersesToMap(mapId, selections);
                    Toast.makeText(this, "Mapa '" + title + "' criado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void addVersesToMap(long mapId, List<VerseSelection> selections) {
        SQLiteDatabase db = BibliaApplication.getDatabaseManager().getReferenceMapDatabase();
        SQLiteDatabase bibleDb = BibliaApplication.getDatabaseManager().getBibleDatabase();
        ReferenceNodeDao nodeDao = new ReferenceNodeDao(db);
        HighlightDao highlightDao = new HighlightDao(bibleDb);

        int added = 0;
        for (VerseSelection vs : selections) {
            ReferenceNode existing = nodeDao.findByReference(mapId, vs.bookId, vs.chapter, vs.verse);
            if (existing == null) {
                ReferenceNode node = new ReferenceNode();
                node.setMapId(mapId);
                node.setBookId(vs.bookId);
                node.setBookName(vs.bookName);
                node.setChapter(vs.chapter);
                node.setVerse(vs.verse);
                node.setVerseText(vs.text);
                node.setTranslation("ARA");
                node.setPositionX(0);
                node.setPositionY(0);

                // Check for existing Bible highlight color
                if (highlightDao.isHighlighted(vs.bookId, vs.chapter, vs.verse)) {
                    // Get the highlight color from Bible
                    List<com.biblia.estudo.model.Highlight> highlights = highlightDao.getByChapter(vs.bookId, vs.chapter);
                    for (com.biblia.estudo.model.Highlight h : highlights) {
                        if (vs.verse >= h.getVerseStart() && vs.verse <= h.getVerseEnd()) {
                            node.setColor(h.getColor());
                            break;
                        }
                    }
                }

                nodeDao.insert(node);
                added++;
            }
        }

        // Update map counts
        ReferenceMapDao mapDao = new ReferenceMapDao(db);
        int nodeCount = nodeDao.getCountByMap(mapId);
        ReferenceEdgeDao edgeDao = new ReferenceEdgeDao(db);
        int edgeCount = edgeDao.getCountByMap(mapId);
        mapDao.updateCounts(mapId, nodeCount, edgeCount);

        Toast.makeText(this, added + " versículo(s) adicionado(s) ao mapa", Toast.LENGTH_SHORT).show();
        clearSelection();
        if (actionMode != null) actionMode.finish();
    }

    static class VerseSelection {
        long bookId;
        String bookName;
        int chapter;
        int verse;
        String text;

        VerseSelection(long bookId, String bookName, int chapter, int verse, String text) {
            this.bookId = bookId;
            this.bookName = bookName;
            this.chapter = chapter;
            this.verse = verse;
            this.text = text;
        }
    }
}
