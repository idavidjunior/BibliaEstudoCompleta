package com.biblia.estudo.ui.highlights;

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
import com.biblia.estudo.data.HighlightDao;
import com.biblia.estudo.data.VerseDao;
import com.biblia.estudo.model.Highlight;
import com.biblia.estudo.ui.bible.BibleReaderActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HighlightsActivity extends Activity {

    private ListView highlightsList;
    private TextView emptyView;
    private Spinner sortSpinner;
    private HighlightDao highlightDao;
    private VerseDao verseDao;
    private HighlightsAdapter adapter;
    private List<Highlight> allHighlights;
    private List<Highlight> displayedHighlights;

    private static final int SORT_BY_DATE = 0;
    private static final int SORT_BY_BOOK = 1;
    private static final int SORT_BY_TESTAMENT = 2;
    private static final int SORT_BY_COLOR = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BibliaApplication.getThemeManager().applyTheme(this);
        setContentView(R.layout.activity_highlights);

        highlightsList = findViewById(R.id.highlightsList);
        emptyView = findViewById(R.id.emptyView);
        sortSpinner = findViewById(R.id.sortSpinner);

        highlightDao = new HighlightDao(BibliaApplication.getDatabaseManager().getBibleDatabase());
        verseDao = new VerseDao(BibliaApplication.getDatabaseManager().getBibleDatabase());

        setupSortSpinner();
        loadHighlights();
    }

    private void setupSortSpinner() {
        String[] options = {
                getString(R.string.sort_by_date),
                getString(R.string.sort_by_book),
                getString(R.string.sort_by_testament),
                getString(R.string.sort_by_color)
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

    private void loadHighlights() {
        allHighlights = highlightDao.getAllWithBookInfo();
        enrichWithVerseText();
        applySort(sortSpinner.getSelectedItemPosition());
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

    private void applySort(int sortType) {
        displayedHighlights = new ArrayList<>(allHighlights);

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

            highlightsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
        }
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