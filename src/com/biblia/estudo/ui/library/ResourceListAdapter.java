package com.biblia.estudo.ui.library;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.biblia.estudo.R;
import com.biblia.estudo.data.UserResourceDao;
import com.biblia.estudo.model.UserResource;
import com.biblia.estudo.utils.StorageOrigin;

import java.util.List;

public class ResourceListAdapter extends BaseAdapter {

    private Context context;
    private List<UserResource> resources;
    private UserResourceDao dao;

    public ResourceListAdapter(Context context, List<UserResource> resources) {
        this(context, resources, null);
    }

    public ResourceListAdapter(Context context, List<UserResource> resources, UserResourceDao dao) {
        this.context = context;
        this.resources = resources;
        this.dao = dao;
    }

    @Override
    public int getCount() { return resources.size(); }

    @Override
    public UserResource getItem(int position) { return resources.get(position); }

    @Override
    public long getItemId(int position) { return resources.get(position).getId(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    android.R.layout.simple_list_item_2, parent, false);
        }

        UserResource res = getItem(position);
        TextView title = convertView.findViewById(android.R.id.text1);
        TextView subtitle = convertView.findViewById(android.R.id.text2);

        if (res.getId() < 0) {
            title.setText("\u21A9  " + res.getTitle());
            subtitle.setText("");
            return convertView;
        }

        String icon = res.isReferencedFolder() || res.isLocalFolder() ? "\uD83D\uDCC2" : getFileIcon(res.getFileTypeLabel());
        title.setText(icon + "  " + res.getTitle());

        StringBuilder sub = new StringBuilder();
        if (res.isReferencedFolder() || res.isLocalFolder()) {
            sub.append("Pasta");
            if (dao != null) {
                int[] counts = res.isReferencedFolder()
                        ? dao.countChildren(res.getId())
                        : new int[]{0, dao.countByFolder(res.getId())};
                StringBuilder det = new StringBuilder();
                if (counts[0] > 0) {
                    det.append(counts[0]).append(counts[0] == 1 ? " subpasta" : " subpastas");
                }
                if (counts[1] > 0) {
                    if (det.length() > 0) det.append("  •  ");
                    det.append(counts[1]).append(counts[1] == 1 ? " arquivo" : " arquivos");
                }
                if (det.length() > 0) {
                    sub.append("  •  ").append(det);
                }
            }
        } else {
            sub.append(res.getFileTypeLabel());
        }
        String size = formatSize(res.getSize());
        if (!size.isEmpty()) sub.append("  •  ").append(size);
        String origin = StorageOrigin.label(Uri.parse(res.getUri()));
        if (origin != null) sub.append("  •  ").append(origin);

        subtitle.setText(sub.toString());

        return convertView;
    }

    private String getFileIcon(String type) {
        switch (type) {
            case "PDF": return "\uD83D\uDCC4";
            case "DOC": return "\uD83D\uDCDD";
            case "XLS": return "\uD83D\uDCCA";
            case "PPT": return "\uD83D\uDCC8";
            case "TXT": return "\uD83D\uDCDD";
            case "IMG": return "\uD83D\uDDBC";
            case "OUTROS": return "\uD83D\uDCC4";
            default: return "\uD83D\uDCC4";
        }
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "";
        String[] units = {"B", "KB", "MB", "GB"};
        int u = 0;
        double s = bytes;
        while (s >= 1024 && u < units.length - 1) { s /= 1024; u++; }
        return String.format("%.1f %s", s, units[u]);
    }
}
