package com.biblia.estudo.ui.highlights;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.biblia.estudo.R;
import com.biblia.estudo.model.Group;

import java.util.List;

public class GroupAdapter extends BaseAdapter {

    private Context context;
    private List<Group> groups;
    private LayoutInflater inflater;
    private OnGroupActionListener listener;

    public interface OnGroupActionListener {
        void onEditGroup(Group group);
        void onDeleteGroup(Group group);
    }

    public GroupAdapter(Context context, List<Group> groups, OnGroupActionListener listener) {
        this.context = context;
        this.groups = groups;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @Override
    public int getCount() { return groups.size(); }

    @Override
    public Group getItem(int position) { return groups.get(position); }

    @Override
    public long getItemId(int position) { return groups.get(position).getId(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_group, parent, false);
            holder = new ViewHolder();
            holder.groupName = convertView.findViewById(R.id.groupName);
            holder.groupItemCount = convertView.findViewById(R.id.groupItemCount);
            holder.btnEdit = convertView.findViewById(R.id.btnEditGroup);
            holder.btnDelete = convertView.findViewById(R.id.btnDeleteGroup);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Group group = groups.get(position);
        holder.groupName.setText(group.getName());

        // TODO: Add item count logic if needed
        holder.groupItemCount.setText("");

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditGroup(group);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteGroup(group);
        });

        return convertView;
    }

    static class ViewHolder {
        TextView groupName;
        TextView groupItemCount;
        ImageView btnEdit;
        ImageView btnDelete;
    }
}