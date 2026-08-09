package com.biblia.estudo.ui.favorites;

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
import com.biblia.estudo.data.FavoriteDao;
import com.biblia.estudo.data.GroupDao;
import com.biblia.estudo.model.Favorite;
import com.biblia.estudo.model.Group;
import com.biblia.estudo.ui.bible.BibleReaderActivity;
import com.biblia.estudo.ui.highlights.GroupManagerActivity;
import com.biblia.estudo.ui.library.HomeActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavoritesActivity extends Activity {

    private ListView favoritesList;
    private TextView emptyView;
    private Spinner sortSpinner;
    private Spinner groupSpinner;
    private ImageView btnBack;
    private ImageView btnHome;

    private FavoriteDao favoriteDao;
    private GroupDao groupDao;
    private FavoritesAdapter adapter;
    private List<Favorite> allFavorites;
    private List<Favorite> displayedFavorites;
    private List<Group> groups;
    private ArrayAdapter<Group> groupAdapter;

    private static final int SORT_BY_DATE = 0;
    private static final int SORT_BY_BOOK = 1;
    private static final int SORT_BY_TESTAMENT = 2;
    private static final int SORT_BY_TAG = 3;
    private static final int SORT_BY_GROUP = 4;

    private static final int REQUEST_GROUP_MANAGER = 1001;

    private ActionMode actionMode;
    private FavoriteActionModeCallback actionModeCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BibliaApplication.getThemeManager().applyTheme(this);
        setContentView(R.layout.activity_favorites);

        favoritesList = findViewById(R.id.favoritesList);
        emptyView = findViewById(R.id.emptyView);
        sortSpinner = findViewById(R.id.sortSpinner);
        groupSpinner = findViewById(R.id.groupSpinner);
        btnBack = findViewById(R.id.btnBack);
        btnHome = findViewById(R.id.btnHome);

        favoriteDao = new FavoriteDao(BibliaApplication.getDatabaseManager().getBibleDatabase());
        groupDao = new GroupDao(BibliaApplication.getDatabaseManager().getBibleDatabase());

        setupToolbar();
        setupSpinners();
        loadGroups();
        loadFavorites();
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
                getString(R.string.sort_by_tag),
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
        groups = groupDao.getAll(Group.TYPE_FAVORITES);
        updateGroupSpinner();
    }

    private void updateGroupSpinner() {
        List<Group> spinnerGroups = new ArrayList<>();
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

    private void loadFavorites() {
        allFavorites = favoriteDao.getAll();
        filterByGroup();
    }

    private void filterByGroup() {
        long selectedGroupId = 0;
        if (groupSpinner.getSelectedItem() instanceof Group) {
            selectedGroupId = ((Group) groupSpinner.getSelectedItem()).getId();
        }

        if (selectedGroupId == 0) {
            displayedFavorites = new ArrayList<>(allFavorites);
        } else {
            displayedFavorites = new ArrayList<>();
            for (Favorite fav : allFavorites) {
                if (fav.getGroupId() == selectedGroupId) {
                    displayedFavorites.add(fav);
                }
            }
        }

        applySort(sortSpinner.getSelectedItemPosition());
    }

    private void applySort(int sortType) {
        switch (sortType) {
            case SORT_BY_DATE:
                Collections.sort(displayedFavorites, (a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
                break;
            case SORT_BY_BOOK:
                Collections.sort(displayedFavorites, (a, b) -> {
                    int bookCompare = a.getBookName().compareTo(b.getBookName());
                    if (bookCompare != 0) return bookCompare;
                    int chapterCompare = Integer.compare(a.getChapter(), b.getChapter());
                    if (chapterCompare != 0) return chapterCompare;
                    return Integer.compare(a.getVerseNumber(), b.getVerseNumber());
                });
                break;
            case SORT_BY_TESTAMENT:
                Collections.sort(displayedFavorites, (a, b) -> {
                    int testamentA = getTestamentForBook(a.getBookName());
                    int testamentB = getTestamentForBook(b.getBookName());
                    int testamentCompare = Integer.compare(testamentA, testamentB);
                    if (testamentCompare != 0) return testamentCompare;
                    int bookCompare = a.getBookName().compareTo(b.getBookName());
                    if (bookCompare != 0) return bookCompare;
                    int chapterCompare = Integer.compare(a.getChapter(), b.getChapter());
                    if (chapterCompare != 0) return chapterCompare;
                    return Integer.compare(a.getVerseNumber(), b.getVerseNumber());
                });
                break;
            case SORT_BY_TAG:
                Collections.sort(displayedFavorites, (a, b) -> {
                    String tagA = a.getTags() != null ? a.getTags() : "";
                    String tagB = b.getTags() != null ? b.getTags() : "";
                    int tagCompare = tagA.compareTo(tagB);
                    if (tagCompare != 0) return tagCompare;
                    int bookCompare = a.getBookName().compareTo(b.getBookName());
                    if (bookCompare != 0) return bookCompare;
                    int chapterCompare = Integer.compare(a.getChapter(), b.getChapter());
                    if (chapterCompare != 0) return chapterCompare;
                    return Integer.compare(a.getVerseNumber(), b.getVerseNumber());
                });
                break;
            case SORT_BY_GROUP:
                Collections.sort(displayedFavorites, (a, b) -> {
                    long groupA = a.getGroupId();
                    long groupB = b.getGroupId();
                    if (groupA != groupB) return Long.compare(groupA, groupB);
                    int bookCompare = a.getBookName().compareTo(b.getBookName());
                    if (bookCompare != 0) return bookCompare;
                    int chapterCompare = Integer.compare(a.getChapter(), b.getChapter());
                    if (chapterCompare != 0) return chapterCompare;
                    return Integer.compare(a.getVerseNumber(), b.getVerseNumber());
                });
                break;
        }

        updateList();
    }

    private int getTestamentForBook(String bookName) {
        String[] otBooks = {"Gênesis", "Êxodo", "Levítico", "Números", "Deuteronômio", "Josué", "Juízes", "Rute",
                "1 Samuel", "2 Samuel", "1 Reis", "2 Reis", "1 Crônicas", "2 Crônicas", "Esdras", "Neemias",
                "Ester", "Jó", "Salmos", "Provérbios", "Eclesiastes", "Cânticos", "Isaías", "Jeremias",
                "Lamentações", "Ezequiel", "Daniel", "Oséias", "Joel", "Amós", "Obadias", "Jonas", "Miquéias",
                "Naum", "Habacuque", "Sofonias", "Ageu", "Zacarias", "Malaquias"};
        String[] ntBooks = {"Mateus", "Marcos", "Lucas", "João", "Atos", "Romanos", "1 Coríntios", "2 Coríntios",
                "Gálatas", "Efésios", "Filipenses", "Colossenses", "1 Tessalonicenses", "2 Tessalonicenses",
                "1 Timóteo", "2 Timóteo", "Tito", "Filemom", "Hebreus", "Tiago", "1 Pedro", "2 Pedro",
                "1 João", "2 João", "3 João", "Judas", "Apocalipse"};

        for (String ot : otBooks) {
            if (bookName.startsWith(ot)) return 1;
        }
        for (String nt : ntBooks) {
            if (bookName.startsWith(nt)) return 2;
        }
        return 3;
    }

    private void updateList() {
        if (displayedFavorites.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            favoritesList.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            favoritesList.setVisibility(View.VISIBLE);
            adapter = new FavoritesAdapter(this, displayedFavorites);
            favoritesList.setAdapter(adapter);

            favoritesList.setOnItemClickListener((parent, view, position, id) -> {
                if (actionMode != null) {
                    toggleSelection(position);
                } else {
                    Favorite fav = adapter.getItem(position);
                    Intent intent = new Intent(FavoritesActivity.this, BibleReaderActivity.class);
                    intent.putExtra("book_id", fav.getBookId());
                    intent.putExtra("book_name", fav.getBookName());
                    intent.putExtra("chapter_count", 150);
                    intent.putExtra("chapter", fav.getChapter());
                    intent.putExtra("verse", fav.getVerseNumber());
                    startActivity(intent);
                }
            });

            favoritesList.setOnItemLongClickListener((parent, view, position, id) -> {
                if (actionMode == null) {
                    actionModeCallback = new FavoriteActionModeCallback();
                    actionMode = startActionMode(actionModeCallback);
                }
                toggleSelection(position);
                return true;
            });

            favoritesList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            favoritesList.setMultiChoiceModeListener(actionModeCallback);
        }
    }

    private void toggleSelection(int position) {
        if (actionMode != null) {
            favoritesList.setItemChecked(position, !favoritesList.isItemChecked(position));
            int checkedCount = favoritesList.getCheckedItemCount();
            actionMode.setTitle(checkedCount + " selecionado(s)");
        }
    }

    private class FavoriteActionModeCallback implements ListView.MultiChoiceModeListener {

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            int checkedCount = favoritesList.getCheckedItemCount();
            mode.setTitle(checkedCount + " selecionado(s)");
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.favorite_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            SparseBooleanArray checked = favoritesList.getCheckedItemPositions();
            List<Long> selectedIds = new ArrayList<>();
            for (int i = 0; i < checked.size(); i++) {
                if (checked.valueAt(i)) {
                    int pos = checked.keyAt(i);
                    if (pos < displayedFavorites.size()) {
                        selectedIds.add(displayedFavorites.get(pos).getId());
                    }
                }
            }

            if (item.getItemId() == R.id.action_move_to_group) {
                showMoveToGroupDialog(selectedIds);
                mode.finish();
                return true;
            } else if (item.getItemId() == R.id.action_remove) {
                removeSelectedFavorites(selectedIds);
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
            favoritesList.clearChoices();
            adapter.notifyDataSetChanged();
        }
    }

    private void showMoveToGroupDialog(List<Long> favoriteIds) {
        if (favoriteIds.isEmpty()) return;

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
            int moved = favoriteDao.moveToGroup(favoriteIds, targetGroupId);
            Toast.makeText(this, getString(R.string.items_moved, moved), Toast.LENGTH_SHORT).show();
            loadFavorites();
            dialog.dismiss();
        });

        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        builder.show();
    }

    private void removeSelectedFavorites(List<Long> favoriteIds) {
        int removed = 0;
        for (Long id : favoriteIds) {
            favoriteDao.delete(id);
            removed++;
        }
        Toast.makeText(this, removed + " favorito(s) removido(s)", Toast.LENGTH_SHORT).show();
        loadFavorites();
    }

    private void openGroupManager() {
        Intent intent = new Intent(this, GroupManagerActivity.class);
        intent.putExtra("type", Group.TYPE_FAVORITES);
        startActivityForResult(intent, REQUEST_GROUP_MANAGER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GROUP_MANAGER && resultCode == RESULT_OK) {
            loadGroups();
            loadFavorites();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    public void removeFavorite(int position) {
        if (displayedFavorites != null && position >= 0 && position < displayedFavorites.size()) {
            Favorite fav = displayedFavorites.get(position);
            favoriteDao.delete(fav.getId());
            displayedFavorites.remove(position);
            allFavorites.remove(fav);
            adapter.notifyDataSetChanged();
            if (displayedFavorites.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                favoritesList.setVisibility(View.GONE);
            }
        }
    }
}