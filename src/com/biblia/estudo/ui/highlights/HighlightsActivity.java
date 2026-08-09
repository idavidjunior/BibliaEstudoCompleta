package com.biblia.estudo.ui.highlights;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.biblia.estudo.R;
import com.biblia.estudo.app.BibliaApplication;
import com.biblia.estudo.data.GroupDao;
import com.biblia.estudo.data.HighlightDao;
import com.biblia.estudo.data.VerseDao;
import com.biblia.estudo.model.Group;
import com.biblia.estudo.model.Highlight;
import com.biblia.estudo.ui.bible.BibleReaderActivity;
import com.biblia.estudo.ui.library.HomeActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HighlightsActivity extends Activity {

    private ListView highlightsList;
    private TextView emptyView;
    private Spinner sortSpinner;
    private Spinner groupSpinner;
    private ImageView btnBack;
    private ImageView btnHome;

    private HighlightDao highlightDao;
    private GroupDao groupDao;
    private VerseDao verseDao;
    private HighlightsAdapter adapter;
    private List<Highlight> allHighlights;
    private List<Highlight> displayedHighlights;
    private List<Group> groups;
    private ArrayAdapter<Group> groupAdapter;

    private static final int SORT_BY_DATE = 0;
    private static final int SORT_BY_BOOK = 1;
    private static final int SORT_BY_TESTAMENT = 2;
    private static final int SORT_BY_COLOR = 3;
    private static final int SORT_BY_GROUP = 4;

    private static final int REQUEST_GROUP_MANAGER = 1001;

    private ActionMode actionMode;
    private HighlightActionModeCallback actionModeCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BibliaApplication.getThemeManager().applyTheme(this);
        setContentView(R.layout.activity_highlights);

        highlightsList = findViewById(R.id.highlightsList);
        emptyView = findViewById(R.id.emptyView);
        sortSpinner = findViewById(R.id.sortSpinner);
        groupSpinner = findViewById(R.id.groupSpinner);
        btnBack = findViewById(R.id.btnBack);
        btnHome = findViewById(R.id.btnHome);

        highlightDao = new HighlightDao(BibliaApplication.getDatabaseManager().getBibleDatabase());
        groupDao = new GroupDao(BibliaApplication.getDatabaseManager().getBibleDatabase());
        verseDao = new VerseDao(BibliaApplication.getDatabaseManager().getBibleDatabase());

        setupToolbar();
        setupSpinners();
        loadGroups();
        loadHighlights();
    }

    private void setupToolbar() {
        btnBack.setOnClickListener(v -> finish());
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private void setupSpinners() {
        // Sort spinner
        String[] sortOptions = {
                getString(R.string.sort_by_date),
                getString(R.string.sort_by_book),
                getString(R.string.sort_by_testament),
                getString(R.string.sort_by_color),
                getString(R.string.sort_by_group)
        };
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applySort(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Group spinner
        groupAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>());
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupAdapter);
        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterByGroup();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadGroups() {
        groups = groupDao.getAll(Group.TYPE_HIGHLIGHTS);
        updateGroupSpinner();
    }

    private void updateGroupSpinner() {
        List<Group> spinnerGroups = new ArrayList<>();
        // Add "No group" option
        Group noGroup = new Group();
        noGroup.setId(0);
        noGroup.setName(getString(R.string.no_group_selected));
        spinnerGroups.add(noGroup);
        spinnerGroups.addAll(groups);
        groupAdapter.clear();
        groupAdapter.addAll(spinnerGroups);
        groupAdapter.notifyDataSetChanged();
        // Default to "No group" (position 0)
        groupSpinner.setSelection(0);
    }

    private void loadHighlights() {
        allHighlights = highlightDao.getAllWithBookInfo();
        enrichWithVerseText();
        filterByGroup();
    }

    private void enrichWithVerseText() {
        for (Highlight hl : allHighlights) {
            List<com.biblia.estudo.model.Verse> verses = verseDao.getVersesRange(
                    hl.getBookId(), hl.getChapter(), hl.getVerseStart(), hl.getVerseEnd());
            if (!verses.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (com.biblia.estudo.model.Verse v : verses) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(v.getText());
                }
                hl.setVerseText(sb.toString());
            }
        }
    }

    private void filterByGroup() {
        long selectedGroupId = 0;
        if (groupSpinner.getSelectedItem() instanceof Group) {
            selectedGroupId = ((Group) groupSpinner.getSelectedItem()).getId();
        }

        if (selectedGroupId == 0) {
            displayedHighlights = new ArrayList<>(allHighlights);
        } else {
            displayedHighlights = new ArrayList<>();
            for (Highlight hl : allHighlights) {
                if (hl.getGroupId() == selectedGroupId) {
                    displayedHighlights.add(hl);
                }
            }
        }

        applySort(sortSpinner.getSelectedItemPosition());
    }

    private void applySort(int sortType) {
        switch (sortType) {
            case SORT_BY_DATE:
                Collections.sort(displayedHighlights, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
            case SORT_BY_BOOK:
                Collections.sort(displayedHighlights, (a, b) -> {
                    int bookCompare = a.getBookName().compareTo(b.getBookName());
                    if (bookCompare != 0) return bookCompare;
                    int chapterCompare = Integer.compare(a.getChapter(), b.getChapter());
                    if (chapterCompare != 0) return chapterCompare;
                    return Integer.compare(a.getVerseStart(), b.getVerseStart());
                });
                break;
            case SORT_BY_TESTAMENT:
                Collections.sort(displayedHighlights, (a, b) -> {
                    int testamentCompare = Integer.compare(a.getTestament(), b.getTestament());
                    if (testamentCompare != 0) return testamentCompare;
                    int bookCompare = a.getBookName().compareTo(b.getBookName());
                    if (bookCompare != 0) return bookCompare;
                    int chapterCompare = Integer.compare(a.getChapter(), b.getChapter());
                    if (chapterCompare != 0) return chapterCompare;
                    return Integer.compare(a.getVerseStart(), b.getVerseStart());
                });
                break;
            case SORT_BY_COLOR:
                Collections.sort(displayedHighlights, (a, b) -> {
                    String colorA = a.getColor() != null ? a.getColor() : "";
                    String colorB = b.getColor() != null ? b.getColor() : "";
                    int colorCompare = colorA.compareTo(colorB);
                    if (colorCompare != 0) return colorCompare;
                    int bookCompare = a.getBookName().compareTo(b.getBookName());
                    if (bookCompare != 0) return bookCompare;
                    int chapterCompare = Integer.compare(a.getChapter(), b.getChapter());
                    if (chapterCompare != 0) return chapterCompare;
                    return Integer.compare(a.getVerseStart(), b.getVerseStart());
                });
                break;
            case SORT_BY_GROUP:
                Collections.sort(displayedHighlights, (a, b) -> {
                    long groupA = a.getGroupId();
                    long groupB = b.getGroupId();
                    if (groupA != groupB) return Long.compare(groupA, groupB);
                    int bookCompare = a.getBookName().compareTo(b.getBookName());
                    if (bookCompare != 0) return bookCompare;
                    int chapterCompare = Integer.compare(a.getChapter(), b.getChapter());
                    if (chapterCompare != 0) return chapterCompare;
                    return Integer.compare(a.getVerseStart(), b.getVerseStart());
                });
                break;
        }

        updateList();
    }

    private void updateList() {
        if (displayedHighlights.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            highlightsList.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            highlightsList.setVisibility(View.VISIBLE);
            adapter = new HighlightsAdapter(this, displayedHighlights);
            highlightsList.setAdapter(adapter);

            highlightsList.setOnItemClickListener((parent, view, position, id) -> {
                if (actionMode != null) {
                    // In selection mode, toggle selection
                    toggleSelection(position);
                } else {
                    // Normal click - open Bible reader
                    Highlight hl = adapter.getItem(position);
                    Intent intent = new Intent(HighlightsActivity.this, BibleReaderActivity.class);
                    intent.putExtra("book_id", hl.getBookId());
                    intent.putExtra("book_name", hl.getBookName());
                    intent.putExtra("chapter_count", 150);
                    intent.putExtra("chapter", hl.getChapter());
                    intent.putExtra("verse", hl.getVerseStart());
                    startActivity(intent);
                }
            });

            highlightsList.setOnItemLongClickListener((parent, view, position, id) -> {
                if (actionMode == null) {
                    actionModeCallback = new HighlightActionModeCallback();
                    actionMode = startActionMode(actionModeCallback);
                }
                toggleSelection(position);
                return true;
            });

            highlightsList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            highlightsList.setMultiChoiceModeListener(actionModeCallback);
        }
    }

    private void toggleSelection(int position) {
        if (actionMode != null) {
            highlightsList.setItemChecked(position, !highlightsList.isItemChecked(position));
            int checkedCount = highlightsList.getCheckedItemCount();
            actionMode.setTitle(checkedCount + " selecionado(s)");
        }
    }

    private class HighlightActionModeCallback implements ListView.MultiChoiceModeListener {

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            int checkedCount = highlightsList.getCheckedItemCount();
            mode.setTitle(checkedCount + " selecionado(s)");
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.highlight_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            SparseBooleanArray checked = highlightsList.getCheckedItemPositions();
            List<Long> selectedIds = new ArrayList<>();
            for (int i = 0; i < checked.size(); i++) {
                if (checked.valueAt(i)) {
                    int pos = checked.keyAt(i);
                    if (pos < displayedHighlights.size()) {
                        selectedIds.add(displayedHighlights.get(pos).getId());
                    }
                }
            }

            if (item.getItemId() == R.id.action_move_to_group) {
                showMoveToGroupDialog(selectedIds);
                mode.finish();
                return true;
            } else if (item.getItemId() == R.id.action_remove) {
                removeSelectedHighlights(selectedIds);
                mode.finish();
                return true;
            } else if (item.getItemId() == R.id.action_manage_groups) {
                openGroupManager();
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            highlightsList.clearChoices();
            adapter.notifyDataSetChanged();
        }
    }

    private void showMoveToGroupDialog(List<Long> highlightIds) {
        if (highlightIds.isEmpty()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.move_to_group));

        List<Group> dialogGroups = new ArrayList<>();
        Group noGroup = new Group();
        noGroup.setId(0);
        noGroup.setName(getString(R.string.no_group_selected));
        dialogGroups.add(noGroup);
        dialogGroups.addAll(groups);

        String[] groupNames = new String[dialogGroups.size()];
        for (int i = 0; i < dialogGroups.size(); i++) {
            groupNames[i] = dialogGroups.get(i).getName();
        }

        builder.setSingleChoiceItems(groupNames, 0, (dialog, which) -> {
            long targetGroupId = dialogGroups.get(which).getId();
            int moved = highlightDao.moveToGroup(highlightIds, targetGroupId);
            Toast.makeText(this, getString(R.string.items_moved, moved), Toast.LENGTH_SHORT).show();
            loadHighlights();
            dialog.dismiss();
        });

        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        builder.show();
    }

    private void removeSelectedHighlights(List<Long> highlightIds) {
        int removed = 0;
        for (Long id : highlightIds) {
            // Find the highlight to get book/chapter/verse for deletion
            for (Highlight hl : displayedHighlights) {
                if (hl.getId() == id) {
                    highlightDao.deleteByVerse(hl.getBookId(), hl.getChapter(), hl.getVerseStart());
                    removed++;
                    break;
                }
            }
        }
        Toast.makeText(this, getString(R.string.items_moved, removed) + " removido(s)", Toast.LENGTH_SHORT).show();
        loadHighlights();
    }

    private void openGroupManager() {
        Intent intent = new Intent(this, GroupManagerActivity.class);
        intent.putExtra("type", Group.TYPE_HIGHLIGHTS);
        startActivityForResult(intent, REQUEST_GROUP_MANAGER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GROUP_MANAGER && resultCode == RESULT_OK) {
            loadGroups();
            loadHighlights();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHighlights();
    }

    public void removeHighlight(int position) {
        if (displayedHighlights != null && position >= 0 && position < displayedHighlights.size()) {
            Highlight hl = displayedHighlights.get(position);
            highlightDao.deleteByVerse(hl.getBookId(), hl.getChapter(), hl.getVerseStart());
            displayedHighlights.remove(position);
            allHighlights.remove(hl);
            adapter.notifyDataSetChanged();
            if (displayedHighlights.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                highlightsList.setVisibility(View.GONE);
            }
            Toast.makeText(this, "Destaque removido", Toast.LENGTH_SHORT).show();
        }
    }
}