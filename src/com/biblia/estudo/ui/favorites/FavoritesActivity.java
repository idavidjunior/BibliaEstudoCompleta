package com.biblia.estudo.ui.favorites;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.biblia.estudo.R;
import com.biblia.estudo.app.BibliaApplication;
import com.biblia.estudo.data.FavoriteDao;
import com.biblia.estudo.model.Favorite;
import com.biblia.estudo.ui.bible.BibleReaderActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FavoritesActivity extends Activity {

    private ListView favoritesList;
    private TextView emptyView;
    private Spinner sortSpinner;
    private FavoriteDao favoriteDao;
    private FavoritesAdapter adapter;
    private List<Favorite> allFavorites;
    private List<Favorite> displayedFavorites;

    private static final int SORT_BY_DATE = 0;
    private static final int SORT_BY_BOOK = 1;
    private static final int SORT_BY_TESTAMENT = 2;
    private static final int SORT_BY_TAG = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BibliaApplication.getThemeManager().applyTheme(this);
        setContentView(R.layout.activity_favorites);

        favoritesList = findViewById(R.id.favoritesList);
        emptyView = findViewById(R.id.emptyView);
        sortSpinner = findViewById(R.id.sortSpinner);

        favoriteDao = new FavoriteDao(BibliaApplication.getDatabaseManager().getBibleDatabase());

        setupSortSpinner();
        loadFavorites();
    }

    private void setupSortSpinner() {
        String[] options = {
                getString(R.string.sort_by_date),
                getString(R.string.sort_by_book),
                getString(R.string.sort_by_testament),
                getString(R.string.sort_by_tag)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applySort(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadFavorites() {
        allFavorites = favoriteDao.getAll();
        applySort(sortSpinner.getSelectedItemPosition());
    }

    private void applySort(int sortType) {
        displayedFavorites = new ArrayList<>(allFavorites);

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

            favoritesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
        }
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
