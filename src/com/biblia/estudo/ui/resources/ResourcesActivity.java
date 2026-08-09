package com.biblia.estudo.ui.resources;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.biblia.estudo.R;
import com.biblia.estudo.app.BibliaApplication;
import com.biblia.estudo.data.ResourceFolderDao;
import com.biblia.estudo.data.UserResourceDao;
import com.biblia.estudo.model.ResourceFolder;
import com.biblia.estudo.model.UserResource;
import com.biblia.estudo.ui.library.ResourceListAdapter;
import com.biblia.estudo.utils.ResourceImportMenu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ResourcesActivity extends Activity {

    private static final int REQUEST_FILE = 2001;
    private static final int REQUEST_FOLDER = 2002;
    private static final long BACK_ITEM_ID = -999L;

    private LinearLayout foldersContainer;
    private ListView filesList;
    private TextView emptyText, folderTitle, btnBack, btnImport, btnSort;

    private UserResourceDao resourceDao;
    private ResourceFolderDao folderDao;
    private long currentFolderId = -2; // -2 = raiz, -1 = sem pasta
    private Deque<Long> folderStack = new ArrayDeque<>();
    private boolean materializing = false;
    private int currentSort = UserResourceDao.SORT_NAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BibliaApplication.getThemeManager().applyTheme(this);
        setContentView(R.layout.activity_resources);

        SQLiteDatabase db = BibliaApplication.getDatabaseManager().getBibleDatabase();
        resourceDao = new UserResourceDao(db);
        folderDao = new ResourceFolderDao(db);

        foldersContainer = findViewById(R.id.foldersContainer);
        filesList = findViewById(R.id.filesList);
        emptyText = findViewById(R.id.emptyText);
        folderTitle = findViewById(R.id.folderTitle);
        btnBack = findViewById(R.id.btnBack);
        btnImport = findViewById(R.id.btnImport);
        btnSort = findViewById(R.id.btnSort);

        currentSort = getPreferences(MODE_PRIVATE).getInt("sort_order", UserResourceDao.SORT_NAME);

        btnBack.setOnClickListener(v -> goUp());
        btnImport.setOnClickListener(v ->
                ResourceImportMenu.show(this, this::importFile, this::importFolder, this::createFolder));
        btnSort.setOnClickListener(v -> showSortDialog());
        refreshFolders();
        showRoot();
    }

    /** Cria uma pasta local (legado) na biblioteca. */
    private void createFolder() {
        EditText input = new EditText(this);
        input.setHint("Nome da pasta");
        input.setPadding(40, 20, 40, 20);
        new AlertDialog.Builder(this)
                .setTitle("Criar Nova Pasta")
                .setView(input)
                .setPositiveButton("Criar", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Digite um nome para a pasta", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ResourceFolder folder = new ResourceFolder();
                    folder.setName(name);
                    folder.setCreatedAt(System.currentTimeMillis());
                    folderDao.insert(folder);
                    refreshFolders();
                    Toast.makeText(this, "Pasta '" + name + "' criada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showSortDialog() {
        String[] options = {"Nome", "Tipo", "Tamanho", "Data"};
        new AlertDialog.Builder(this)
                .setTitle("Ordenar por")
                .setSingleChoiceItems(options, currentSort, (dialog, which) -> {
                    currentSort = which;
                    getPreferences(MODE_PRIVATE).edit().putInt("sort_order", currentSort).apply();
                    dialog.dismiss();
                    refreshFiles();
                })
                .show();
    }

    // ---------- Navegação ----------

    private void showRoot() {
        folderStack.clear();
        currentFolderId = -2;
        refreshFiles();
    }

    private void goUp() {
        if (currentFolderId == -2) {
            finish();
            return;
        }
        if (currentFolderId >= 0 && isTreeFolder(currentFolderId)) {
            if (!folderStack.isEmpty()) {
                currentFolderId = folderStack.pop();
            } else {
                currentFolderId = -2;
            }
            refreshFiles();
            return;
        }
        currentFolderId = -2;
        refreshFiles();
    }

    private void enterFolder(UserResource folder) {
        if (currentFolderId >= 0) folderStack.push(currentFolderId);
        currentFolderId = folder.getId();
        refreshFiles();
    }

    private boolean isTreeFolder(long id) {
        UserResource r = resourceDao.getById(id);
        return r != null && r.isReferencedFolder();
    }

    private long targetParentId() {
        if (currentFolderId >= 0 && isTreeFolder(currentFolderId)) return currentFolderId;
        return 0;
    }

    // ---------- Listagem ----------

    private void refreshFiles() {
        UserResource tree = null;
        if (currentFolderId >= 0) {
            tree = resourceDao.getById(currentFolderId);
        }

        List<UserResource> items;
        String title;
        if (tree != null && tree.isReferencedFolder()) {
            items = loadTreeChildren(tree);
            title = "📂  " + tree.getTitle();
        } else if (currentFolderId == -2) {
            items = loadRootItems();
            title = "Todos os arquivos";
        } else if (currentFolderId == -1) {
            items = resourceDao.getByFolder(-1, currentSort);
            title = "Sem pasta";
        } else {
            items = resourceDao.getByFolder(currentFolderId, currentSort);
            ResourceFolder f = folderDao.getById(currentFolderId);
            title = f != null ? (f.getIcon() + "  " + f.getName()) : "Pasta";
        }

        folderTitle.setText(title);
        emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        emptyText.setText(items.isEmpty() ? (materializing ? "Carregando..." : "Nenhum arquivo aqui") : "");

        ResourceListAdapter adapter = new ResourceListAdapter(this, items, resourceDao);
        filesList.setAdapter(adapter);

        filesList.setOnItemClickListener((parent, view, position, id) -> {
            UserResource res = adapter.getItem(position);
            if (res.getId() == BACK_ITEM_ID) {
                goUp();
            } else if (res.isReferencedFolder()) {
                enterFolder(res);
            } else if (res.isLocalFolder()) {
                enterFolder(res);
            } else {
                openFile(res);
            }
        });

        filesList.setOnItemLongClickListener((parent, view, position, id) -> {
            UserResource res = adapter.getItem(position);
            if (res.getId() == BACK_ITEM_ID) return false;
            showItemActions(res);
            return true;
        });
    }

    private List<UserResource> loadRootItems() {
        List<UserResource> items = new ArrayList<>();
        for (UserResource r : resourceDao.getRootResources(currentSort)) {
            // Arquivos legados dentro de pastas locais continuam lá; não aparecem na raiz duplicados.
            if (r.isReferencedFile() && r.getFolderId() >= 0) continue;
            items.add(r);
        }
        materializeRootCounts(items);
        return items;
    }

    /** Garante que pastas referenciadas tenham os filhos persistidos para exibir contagens reais. */
    private void materializeRootCounts(List<UserResource> items) {
        if (materializing) return;
        boolean need = false;
        for (UserResource r : items) {
            if (r.isReferencedFolder()) {
                int[] c = resourceDao.countChildren(r.getId());
                if (c[0] + c[1] == 0) { need = true; break; }
            }
        }
        if (!need) return;
        materializing = true;
        new Thread(() -> {
            for (UserResource r : items) {
                if (r.isReferencedFolder()) {
                    int[] c = resourceDao.countChildren(r.getId());
                    if (c[0] + c[1] == 0) {
                        resourceDao.importChildrenForFolder(r.getId(), getContentResolver());
                    }
                }
            }
            runOnUiThread(() -> {
                materializing = false;
                refreshFiles();
            });
        }).start();
    }

    private List<UserResource> loadTreeChildren(UserResource folder) {
        List<UserResource> children = resourceDao.getChildren(folder.getId(), currentSort);
        if (children.isEmpty() && !materializing) {
            materializing = true;
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText("Carregando...");
            new Thread(() -> {
                int n = resourceDao.importChildrenForFolder(folder.getId(), getContentResolver());
                runOnUiThread(() -> {
                    materializing = false;
                    if (n > 0) {
                        refreshFiles();
                    } else {
                        emptyText.setText("Pasta vazia");
                    }
                });
            }).start();
        }
        List<UserResource> items = new ArrayList<>();
        UserResource back = new UserResource();
        back.setId(BACK_ITEM_ID);
        back.setTitle("← Voltar");
        back.setType(UserResource.TYPE_LOCAL_FOLDER);
        items.add(back);
        items.addAll(children);
        return items;
    }

    // ---------- Pastas locais (legado) ----------

    private void refreshFolders() {
        foldersContainer.removeAllViews();
        List<ResourceFolder> folders = folderDao.getAll();

        if (folders.isEmpty()) {
            foldersContainer.setVisibility(View.GONE);
            return;
        }
        foldersContainer.setVisibility(View.VISIBLE);

        for (ResourceFolder f : folders) {
            View v = getLayoutInflater().inflate(R.layout.item_resource_folder, foldersContainer, false);
            ((TextView) v.findViewById(android.R.id.text1)).setText(f.getIcon() + "  " + f.getName());
            ((TextView) v.findViewById(android.R.id.text2)).setText(f.getItemCount() + " arquivos");

            v.setOnClickListener(click -> {
                currentFolderId = f.getId();
                refreshFiles();
            });

            v.setOnLongClickListener(click -> {
                showFolderActions(f);
                return true;
            });

            foldersContainer.addView(v);
        }
    }

    private void showFolderActions(ResourceFolder f) {
        String[] options = {"Renomear pasta", "Excluir pasta"};
        new AlertDialog.Builder(this)
                .setTitle(f.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) renameFolder(f);
                    else {
                        folderDao.delete(f.getId());
                        refreshFolders();
                        if (currentFolderId == f.getId()) showRoot();
                        Toast.makeText(this, "Pasta excluída", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void renameFolder(ResourceFolder f) {
        EditText input = new EditText(this);
        input.setText(f.getName());
        input.setPadding(40, 20, 40, 20);
        new AlertDialog.Builder(this)
                .setTitle("Renomear pasta")
                .setView(input)
                .setPositiveButton("OK", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        folderDao.updateName(f.getId(), name);
                        refreshFolders();
                        refreshFiles();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ---------- Ações de itens (referências) ----------

    private void showItemActions(UserResource res) {
        if (res.isReferencedFolder()) {
            showReferencedFolderActions(res);
        } else {
            showReferencedFileActions(res);
        }
    }

    private void showReferencedFolderActions(UserResource res) {
        String[] options = {"Abrir", "Renomear", "Remover da biblioteca"};
        new AlertDialog.Builder(this)
                .setTitle(res.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        enterFolder(res);
                    } else if (which == 1) {
                        renameItem(res);
                    } else {
                        removeSubtree(res);
                    }
                }).show();
    }

    private void showReferencedFileActions(UserResource res) {
        String[] options = {"Abrir", "Renomear", "Remover da biblioteca"};
        new AlertDialog.Builder(this)
                .setTitle(res.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openFile(res);
                    } else if (which == 1) {
                        renameItem(res);
                    } else {
                        resourceDao.deleteById(res.getId());
                        refreshFiles();
                        refreshFolders();
                        Toast.makeText(this, "Referência removida da biblioteca", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    /** Remove a pasta e todos os descendentes APENAS da biblioteca (as referências). */
    private void removeSubtree(UserResource res) {
        new AlertDialog.Builder(this)
                .setTitle("Remover da biblioteca?")
                .setMessage("A pasta \"" + res.getTitle() + "\" e todo o seu conteúdo serão removidos apenas da biblioteca. Os arquivos originais no dispositivo não são afetados.")
                .setPositiveButton("Remover", (d, w) -> {
                    resourceDao.deleteSubtree(res.getId());
                    folderStack.remove(res.getId());
                    if (currentFolderId == res.getId()) {
                        currentFolderId = folderStack.isEmpty() ? -2 : folderStack.pop();
                    }
                    refreshFiles();
                    refreshFolders();
                    Toast.makeText(this, "Referência removida da biblioteca", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void renameItem(UserResource res) {
        EditText input = new EditText(this);
        input.setText(res.getTitle());
        input.setPadding(40, 20, 40, 20);
        new AlertDialog.Builder(this)
                .setTitle("Renomear")
                .setView(input)
                .setPositiveButton("OK", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        resourceDao.rename(res.getId(), name);
                        refreshFiles();
                        refreshFolders();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void openFile(UserResource res) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(res.getUri()), res.getMimeType());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao abrir arquivo", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------- Importação ----------

    private void importFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_FILE);
    }

    private void importFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQUEST_FILE) {
            if (data.getData() != null) {
                Uri uri = data.getData();
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
                addResource(uri);
            } else if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
                    addResource(uri);
                }
                Toast.makeText(this, count + " arquivo(s) importado(s)", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_FOLDER) {
            Uri treeUri = data.getData();
            if (treeUri == null) return;
            try { getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            importFolderContents(treeUri);
        }
    }

    private void addResource(Uri uri) {
        String title = extractFileName(uri);
        String mime = getContentResolver().getType(uri);
        if (mime == null) mime = "application/octet-stream";
        long size = extractFileSize(uri);
        UserResource res = new UserResource();
        res.setTitle(title);
        res.setUri(uri.toString());
        res.setMimeType(mime);
        res.setSize(size);
        res.setType(UserResource.TYPE_REFERENCED_FILE);
        if (currentFolderId >= 0 && isTreeFolder(currentFolderId)) {
            res.setParentId(currentFolderId);
            res.setFolderId(-1);
        } else {
            res.setParentId(0);
            res.setFolderId(currentFolderId >= 0 ? currentFolderId : -1);
        }
        res.setCreatedAt(System.currentTimeMillis());
        resourceDao.insert(res);
        refreshFiles();
        refreshFolders();
        Toast.makeText(this, "Importado: " + title, Toast.LENGTH_SHORT).show();
    }

    /** Importa a árvore inteira da pasta escolhida (subpastas + arquivos), preservando nomes. */
    private void importFolderContents(Uri treeUri) {
        new Thread(() -> {
            long id = resourceDao.importFolderTree(getContentResolver(), treeUri, targetParentId());
            runOnUiThread(() -> {
                if (id > 0) {
                    refreshFiles();
                    refreshFolders();
                    Toast.makeText(this, "Pasta importada para a biblioteca", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Erro ao importar pasta", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private String extractFileName(Uri uri) {
        String name = "Arquivo";
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {}
        if (name == null || name.isEmpty()) name = uri.getLastPathSegment();
        return name != null ? name : "Arquivo";
    }

    private long extractFileSize(Uri uri) {
        long size = 0;
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0) size = c.getLong(idx);
            }
        } catch (Exception ignored) {}
        return size;
    }
}
