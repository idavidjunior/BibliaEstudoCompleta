package com.biblia.estudo.ui.search;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.biblia.estudo.R;
import com.biblia.estudo.data.HighlightDao;

public class SearchResultsAdapter extends CursorAdapter {

    private LayoutInflater inflater;
    private boolean selectionMode = false;
    private java.util.Set<Long> selectedIds = new java.util.HashSet<>();
    private HighlightDao highlightDao;
    private long currentBookId;
    private int currentChapter;

    public SearchResultsAdapter(Context context, Cursor cursor, HighlightDao highlightDao) {
        super(context, cursor, 0);
        inflater = LayoutInflater.from(context);
        this.highlightDao = highlightDao;
    }

    public void setSelectionMode(boolean enabled) {
        this.selectionMode = enabled;
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void toggleSelection(long verseId) {
        if (selectedIds.contains(verseId)) {
            selectedIds.remove(verseId);
        } else {
            selectedIds.add(verseId);
        }
        notifyDataSetChanged();
    }

    public java.util.Set<Long> getSelectedIds() {
        return selectedIds;
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public void setCurrentChapterContext(long bookId, int chapter) {
        this.currentBookId = bookId;
        this.currentChapter = chapter;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = inflater.inflate(R.layout.list_item_result, parent, false);
        ViewHolder holder = new ViewHolder();
        holder.resultRef = view.findViewById(R.id.resultRef);
        holder.resultSnippet = view.findViewById(R.id.resultSnippet);
        holder.checkbox = view.findViewById(R.id.resultCheckbox);
        holder.colorIndicator = view.findViewById(R.id.resultColorIndicator);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();

        long verseId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
        String bookName = cursor.getString(cursor.getColumnIndexOrThrow("book_name"));
        int chapter = cursor.getInt(cursor.getColumnIndexOrThrow("chapter"));
        int verse = cursor.getInt(cursor.getColumnIndexOrThrow("verse_number"));
        String text = cursor.getString(cursor.getColumnIndexOrThrow("text"));

        holder.resultRef.setText(bookName + " " + chapter + ":" + verse);
        holder.resultSnippet.setText(text);

        // Checkbox for selection mode
        if (selectionMode) {
            holder.checkbox.setVisibility(View.VISIBLE);
            holder.checkbox.setChecked(selectedIds.contains(verseId));
            holder.checkbox.setOnCheckedChangeListener(null);
            holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedIds.add(verseId);
                } else {
                    selectedIds.remove(verseId);
                }
            });
        } else {
            holder.checkbox.setVisibility(View.GONE);
        }

        // Color indicator from highlights
        String highlightColor = getHighlightColor(bookName, chapter, verse);
        if (highlightColor != null) {
            holder.colorIndicator.setVisibility(View.VISIBLE);
            try {
                holder.colorIndicator.setBackgroundColor(Color.parseColor(highlightColor));
            } catch (Exception ignored) {
                holder.colorIndicator.setVisibility(View.GONE);
            }
        } else {
            holder.colorIndicator.setVisibility(View.GONE);
        }
    }

    private String getHighlightColor(String bookName, int chapter, int verse) {
        if (highlightDao == null) return null;
        // Need to get bookId from bookName - simplified approach
        // In reality we'd need a BookDao lookup, but for now check by chapter/verse context
        if (currentBookId > 0 && currentChapter == chapter) {
            return highlightDao.isHighlighted(currentBookId, chapter, verse) ? "#FFF9C4" : null;
        }
        return null;
    }

    static class ViewHolder {
        TextView resultRef;
        TextView resultSnippet;
        CheckBox checkbox;
        View colorIndicator;
    }
}
