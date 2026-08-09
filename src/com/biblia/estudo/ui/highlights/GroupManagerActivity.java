package com.biblia.estudo.ui.highlights;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.biblia.estudo.R;
import com.biblia.estudo.app.BibliaApplication;
import com.biblia.estudo.data.GroupDao;
import com.biblia.estudo.model.Group;

import java.util.ArrayList;
import java.util.List;

public class GroupManagerActivity extends Activity implements GroupAdapter.OnGroupActionListener {

    private ListView groupsList;
    private TextView emptyView;
    private GroupDao groupDao;
    private GroupAdapter adapter;
    private List<Group> groups;
    private int type; // Group.TYPE_HIGHLIGHTS or Group.TYPE_FAVORITES

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BibliaApplication.getThemeManager().applyTheme(this);
        setContentView(R.layout.activity_group_manager);

        type = getIntent().getIntExtra("type", Group.TYPE_HIGHLIGHTS);

        groupsList = findViewById(R.id.groupsList);
        emptyView = findViewById(R.id.emptyView);

        groupDao = new GroupDao(BibliaApplication.getDatabaseManager().getBibleDatabase());

        setupToolbar();
        loadGroups();

        findViewById(R.id.btnAddGroup).setOnClickListener(v -> showCreateGroupDialog());

        groupsList.setOnItemClickListener((parent, view, position, id) -> {
            Group group = adapter.getItem(position);
            // Return selected group to caller
            Intent result = new Intent();
            result.putExtra("group_id", group.getId());
            result.putExtra("group_name", group.getName());
            setResult(RESULT_OK, result);
            finish();
        });
    }

    private void setupToolbar() {
        TextView title = findViewById(R.id.toolbarTitle);
        String titleText = type == Group.TYPE_HIGHLIGHTS ? getString(R.string.manage_groups) + " - " + getString(R.string.title_highlights) :
                getString(R.string.manage_groups) + " - " + getString(R.string.title_favorites);
        title.setText(titleText);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.biblia.estudo.ui.library.HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private void loadGroups() {
        groups = groupDao.getAll(type);
        if (groups.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            groupsList.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            groupsList.setVisibility(View.VISIBLE);
            adapter = new GroupAdapter(this, groups, this);
            groupsList.setAdapter(adapter);
        }
    }

    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.create_group));

        final EditText input = new EditText(this);
        input.setHint(getString(R.string.group_name_hint));
        input.setPadding(40, 20, 40, 20);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.btn_save), (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.group_name_hint), Toast.LENGTH_SHORT).show();
                return;
            }
            Group group = new Group();
            group.setName(name);
            group.setType(type);
            group.setOrder(groups.size());
            long id = groupDao.insert(group);
            if (id > 0) {
                group.setId(id);
                groups.add(group);
                adapter.notifyDataSetChanged();
                emptyView.setVisibility(View.GONE);
                groupsList.setVisibility(View.VISIBLE);
                Toast.makeText(this, getString(R.string.group_created), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        builder.show();
    }

    @Override
    public void onEditGroup(Group group) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.edit_group));

        final EditText input = new EditText(this);
        input.setText(group.getName());
        input.setPadding(40, 20, 40, 20);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.btn_save), (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) return;
            group.setName(name);
            groupDao.update(group);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, getString(R.string.group_created), Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        builder.show();
    }

    @Override
    public void onDeleteGroup(Group group) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_group))
                .setMessage(getString(R.string.confirm_delete_group))
                .setPositiveButton(getString(R.string.btn_delete), (dialog, which) -> {
                    groupDao.delete(group.getId());
                    groups.remove(group);
                    adapter.notifyDataSetChanged();
                    if (groups.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        groupsList.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, getString(R.string.group_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }
}