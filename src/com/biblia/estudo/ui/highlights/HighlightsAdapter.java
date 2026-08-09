package com.biblia.estudo.ui.highlights;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.biblia.estudo.R;
import com.biblia.estudo.model.Highlight;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HighlightsAdapter extends BaseAdapter {

    private Context context;
    private List<Highlight> highlights;
    private LayoutInflater inflater;
    private SimpleDateFormat dateFormat;
    private SparseBooleanArray selectedItems;

    public HighlightsAdapter(Context context, List<Highlight> highlights) {
        this.context = context;
        this.highlights = highlights;
        this.inflater = LayoutInflater.from(context);
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        this.selectedItems = new SparseBooleanArray();
    }

    public void setSelectedItems(SparseBooleanArray selectedItems) {
        this.selectedItems = selectedItems;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() { return highlights.size(); }

    @Override
    public Highlight getItem(int position) { return highlights.get(position); }

    @Override
    public long getItemId(int position) { return highlights.get(position).getId(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_highlight, parent, false);
            holder = new ViewHolder();
            holder.colorIndicator = convertView.findViewById(R.id.colorIndicator);
            holder.hlRef = convertView.findViewById(R.id.hlRef);
            holder.hlSnippet = convertView.findViewById(R.id.hlSnippet);
            holder.hlMeta = convertView.findViewById(R.id.hlMeta);
            holder.btnRemove = convertView.findViewById(R.id.btnRemoveHl);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Highlight hl = highlights.get(position);
        holder.hlRef.setText(hl.getReference());
        holder.hlSnippet.setText(hl.getVerseText() != null ? hl.getVerseText() : "");

        // Color indicator
        try {
            holder.colorIndicator.setBackgroundColor(Color.parseColor(hl.getColor()));
        } catch (Exception ignored) {
            holder.colorIndicator.setBackgroundColor(Color.parseColor("#FFF9C4"));
        }

        // Meta info: date + testament + color name
        String meta = dateFormat.format(new Date(hl.getCreatedAt()));
        if (hl.getTestamentName() != null && !hl.getTestamentName().isEmpty()) {
            meta += " • " + hl.getTestamentName();
        }
        meta += " • " + hl.getColorName();
        holder.hlMeta.setText(meta);

        // Highlight selected items
        boolean isSelected = selectedItems.get(position, false);
        if (isSelected) {
            convertView.setBackgroundColor(Color.parseColor("#E3F2FD"));
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        final int pos = position;
        holder.btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof HighlightsActivity) {
                    ((HighlightsActivity) context).removeHighlight(pos);
                }
            }
        });

        return convertView;
    }

    static class ViewHolder {
        View colorIndicator;
        TextView hlRef;
        TextView hlSnippet;
        TextView hlMeta;
        TextView btnRemove;
    }
}