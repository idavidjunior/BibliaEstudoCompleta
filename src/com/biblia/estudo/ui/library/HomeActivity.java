package com.biblia.estudo.ui.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.biblia.estudo.R;
import com.biblia.estudo.app.BibliaApplication;
import com.biblia.estudo.data.BookDao;
import com.biblia.estudo.data.ReadingProgressDao;
import com.biblia.estudo.data.ResourceFolderDao;
import com.biblia.estudo.data.UserResourceDao;
import com.biblia.estudo.data.VerseDao;
import com.biblia.estudo.model.Book;
import com.biblia.estudo.model.ReadingProgress;
import com.biblia.estudo.model.UserResource;
import com.biblia.estudo.model.Verse;
import com.biblia.estudo.ui.bible.BibleReaderActivity;
import com.biblia.estudo.ui.crossref.ReferenceMapActivity;
import com.biblia.estudo.ui.dictionary.DictionaryActivity;
import com.biblia.estudo.ui.notes.NotesActivity;
import com.biblia.estudo.ui.resources.ResourcesActivity;
import com.biblia.estudo.utils.NavigationHelper;
import com.biblia.estudo.utils.ResourceImportMenu;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {

    private static final int REQUEST_IMPORT_FOLDER = 1002;
    private static final int REQUEST_IMPORT_MULTIPLE_FILES = 1003;

    private TextView lastReadingRef, lastReadingText;
    private Button btnContinue;
    private TextView verseOfDayText, verseOfDayRef;
    private FrameLayout resourcesSection, notesSection, crossRefSection;
    private ListView resourceList;
    private View emptyResources;
    private TextView notesPreview, crossRefPreview;

    private ReadingProgressDao progressDao;
    private BookDao bookDao;
    private VerseDao verseDao;
    private UserResourceDao resourceDao;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_reorder_home) {
            showReorderDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BibliaApplication.getThemeManager().applyTheme(this);
        setContentView(R.layout.activity_home);
        setActionBar(findViewById(R.id.toolbar));

        SQLiteDatabase db = BibliaApplication.getDatabaseManager().getBibleDatabase();
        progressDao = new ReadingProgressDao(db);
        bookDao = new BookDao(db);
        verseDao = new VerseDao(db);
        resourceDao = new UserResourceDao(db);

        lastReadingRef = findViewById(R.id.lastReadingRef);
        lastReadingText = findViewById(R.id.lastReadingText);
        btnContinue = findViewById(R.id.btnContinueReading);
        verseOfDayText = findViewById(R.id.verseOfDayText);
        verseOfDayRef = findViewById(R.id.verseOfDayRef);
        resourcesSection = findViewById(R.id.resourcesSection);
        notesSection = findViewById(R.id.notesSection);
        crossRefSection = findViewById(R.id.crossRefSection);
        resourceList = findViewById(R.id.resourceList);
        emptyResources = findViewById(R.id.emptyResources);
        notesPreview = findViewById(R.id.notesPreview);
        crossRefPreview = findViewById(R.id.crossRefPreview);

        NavigationHelper.setupBottomNav(this, R.id.navHome);
        setupLastReading();
        setupVerseOfDay();
        setupResources();
        setupNotes();
        setupCrossRefs();
        setupDictionary();
        applyHomeOrder();
        setupReorderByLongPress();

        findViewById(R.id.btnImport).setOnClickListener(v ->
                ResourceImportMenu.show(this, this::importFile, this::importFolder, this::createFolder));
        findViewById(R.id.resourcesHeader).setOnClickListener(v -> {
            startActivity(new Intent(this, ResourcesActivity.class));
        });
        findViewById(R.id.btnOpenResources).setOnClickListener(v -> {
            startActivity(new Intent(this, ResourcesActivity.class));
        });
        findViewById(R.id.btnOpenNotes).setOnClickListener(v -> {
            startActivity(new Intent(this, NotesActivity.class));
        });
        notesSection.setOnClickListener(v -> {
            startActivity(new Intent(this, NotesActivity.class));
        });

        findViewById(R.id.quickBible).setOnClickListener(v ->
                startActivity(new Intent(this, LibraryActivity.class)));
        findViewById(R.id.quickSearch).setOnClickListener(v ->
                startActivity(new Intent(this, com.biblia.estudo.ui.search.SearchActivity.class)));
        findViewById(R.id.quickFavorites).setOnClickListener(v ->
                startActivity(new Intent(this, com.biblia.estudo.ui.favorites.FavoritesActivity.class)));
    }

    private void createFolder() {
        android.widget.EditText input = new android.widget.EditText(this);
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
                    com.biblia.estudo.model.ResourceFolder folder = new com.biblia.estudo.model.ResourceFolder();
                    folder.setName(name);
                    folder.setCreatedAt(System.currentTimeMillis());
                    ResourceFolderDao folderDao = new ResourceFolderDao(BibliaApplication.getDatabaseManager().getBibleDatabase());
                    folderDao.insert(folder);
                    Toast.makeText(this, "Pasta '" + name + "' criada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void setupLastReading() {
        ReadingProgress last = progressDao.getLastReading();
        if (last != null) {
            Book book = bookDao.getById(last.getBookId());
            if (book != null) {
                String ref = book.getName() + " " + last.getChapter() + ":" + last.getVerse();
                lastReadingRef.setText(ref);

                List<Verse> verses = verseDao.getVersesRange(
                        last.getBookId(), last.getChapter(), last.getVerse(), last.getVerse(),
                        com.biblia.estudo.app.BibliaApplication.getActiveTranslation());
                if (!verses.isEmpty()) {
                    String text = verses.get(0).getText();
                    if (text.length() > 100) text = text.substring(0, 100) + "...";
                    lastReadingText.setText(text);
                }

                btnContinue.setOnClickListener(v -> {
                    Intent intent = new Intent(HomeActivity.this, BibleReaderActivity.class);
                    intent.putExtra("book_id", last.getBookId());
                    intent.putExtra("book_name", book.getName());
                    intent.putExtra("chapter_count", book.getChapterCount());
                    intent.putExtra("chapter", last.getChapter());
                    intent.putExtra("verse", last.getVerse());
                    startActivity(intent);
                });

                findViewById(R.id.lastReadingCard).setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupVerseOfDay() {
        SQLiteDatabase db = BibliaApplication.getDatabaseManager().getBibleDatabase();
        android.database.Cursor countC = db.rawQuery(
                "SELECT COUNT(*) FROM verses v JOIN books b ON v.book_id = b._id", null);
        int total = 0;
        if (countC != null && countC.moveToFirst()) {
            total = countC.getInt(0);
        }
        if (countC != null) countC.close();

        if (total <= 0) return;

        int offset = (int) (Math.random() * total);
        android.database.Cursor c = db.rawQuery(
                "SELECT v.text, v.chapter, v.verse_number, b.name FROM verses v " +
                        "JOIN books b ON v.book_id = b._id LIMIT 1 OFFSET ?",
                new String[]{String.valueOf(offset)});
        if (c != null && c.moveToFirst()) {
            String text = c.getString(0);
            int chapter = c.getInt(1);
            int verse = c.getInt(2);
            String bookName = c.getString(3);
            verseOfDayText.setText("\"" + text + "\"");
            verseOfDayRef.setText(bookName + " " + chapter + ":" + verse);
            c.close();
        } else if (c != null) {
            c.close();
        }
    }

    private void setupDictionary() {
        findViewById(R.id.dictionarySection).setOnClickListener(v -> {
            startActivity(new Intent(this, DictionaryActivity.class));
        });
        findViewById(R.id.btnOpenDictionary).setOnClickListener(v -> {
            startActivity(new Intent(this, DictionaryActivity.class));
        });
    }

    private void setupReorderByLongPress() {
        for (View section : getHomeSections()) {
            section.setOnLongClickListener(v -> {
                showReorderDialog();
                return true;
            });
        }
    }

    private List<View> getHomeSections() {
        List<View> sections = new ArrayList<>();
        sections.add(findViewById(R.id.quickShortcutsRow));
        sections.add(findViewById(R.id.lastReadingCard));
        sections.add(findViewById(R.id.verseOfDayCard));
        sections.add(findViewById(R.id.dictionarySection));
        sections.add(findViewById(R.id.notesSection));
        sections.add(findViewById(R.id.crossRefSection));
        sections.add(findViewById(R.id.resourcesSection));
        return sections;
    }

    private String sectionTitle(View v) {
        int id = v.getId();
        if (id == R.id.quickShortcutsRow) return "Atalhos";
        if (id == R.id.lastReadingCard) return getString(R.string.last_reading);
        if (id == R.id.verseOfDayCard) return getString(R.string.verse_of_day);
        if (id == R.id.dictionarySection) return getString(R.string.title_dictionary);
        if (id == R.id.notesSection) return getString(R.string.title_notes);
        if (id == R.id.crossRefSection) return getString(R.string.title_cross_references);
        if (id == R.id.resourcesSection) return getString(R.string.my_resources);
        return "";
    }

    private void applyHomeOrder() {
        String saved = BibliaApplication.getHomeOrder();
        if (saved == null || saved.isEmpty()) return;

        LinearLayout container = findViewById(R.id.homeSectionsContainer);
        String[] order = saved.split(",");
        List<View> sections = getHomeSections();
        for (String idStr : order) {
            try {
                int id = Integer.parseInt(idStr.trim());
                for (View v : sections) {
                    if (v.getId() == id) {
                        container.removeView(v);
                        container.addView(v);
                        break;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private void showReorderDialog() {
        final List<View> sections = getHomeSections();
        final List<Integer> order = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) order.add(i);

        final List<String> titles = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) titles.add(sectionTitle(sections.get(i)));

        final ListView list = new ListView(this);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, titles);
        list.setAdapter(adapter);

        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position > 0) {
                int idx = order.get(position);
                order.remove(position);
                order.add(position - 1, idx);
                String t = titles.remove(position);
                titles.add(position - 1, t);
                adapter.notifyDataSetChanged();
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Personalizar ordem dos atalhos")
                .setMessage("Toque em um item para movê-lo para cima.")
                .setView(list)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < order.size(); i++) {
                        if (i > 0) sb.append(',');
                        sb.append(sections.get(order.get(i)).getId());
                    }
                    BibliaApplication.setHomeOrder(sb.toString());
                    applyHomeOrder();
                    Toast.makeText(this, "Ordem dos atalhos atualizada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void setupResources() {
        refreshResources();
        resourceList.setOnItemClickListener((parent, view, position, id) -> {
            UserResource res = (UserResource) parent.getItemAtPosition(position);
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(res.getUri()), res.getMimeType());
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Erro ao abrir arquivo", Toast.LENGTH_SHORT).show();
            }
        });
        resourceList.setOnItemLongClickListener((parent, view, position, id) -> {
            UserResource res = (UserResource) parent.getItemAtPosition(position);
            if (res.isReferencedFolder()) {
                resourceDao.deleteSubtree(res.getId());
            } else {
                resourceDao.deleteById(res.getId());
            }
            refreshResources();
            Toast.makeText(this, "Referência removida da biblioteca", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void refreshResources() {
        List<UserResource> resources = resourceDao.getAll();
        resourcesSection.setVisibility(View.VISIBLE);
        if (resources.isEmpty()) {
            emptyResources.setVisibility(View.VISIBLE);
            resourceList.setVisibility(View.GONE);
        } else {
            emptyResources.setVisibility(View.GONE);
            resourceList.setVisibility(View.VISIBLE);
            resourceList.setAdapter(new com.biblia.estudo.ui.library.ResourceListAdapter(this, resources, resourceDao));
        }
    }

    private void setupNotes() {
        notesSection.setOnClickListener(v -> {
            startActivity(new Intent(this, NotesActivity.class));
        });
        notesPreview.setOnClickListener(v -> {
            startActivity(new Intent(this, NotesActivity.class));
        });
    }

    private void setupCrossRefs() {
        crossRefSection.setOnClickListener(v -> {
            startActivity(new Intent(this, ReferenceMapActivity.class));
        });
        crossRefPreview.setOnClickListener(v -> {
            startActivity(new Intent(this, ReferenceMapActivity.class));
        });
        findViewById(R.id.crossRefHeader).setOnClickListener(v -> {
            startActivity(new Intent(this, ReferenceMapActivity.class));
        });
        findViewById(R.id.btnOpenCrossRefs).setOnClickListener(v -> {
            startActivity(new Intent(this, ReferenceMapActivity.class));
        });
    }

    private void importFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_IMPORT_MULTIPLE_FILES);
    }

    private void importFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_IMPORT_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQUEST_IMPORT_MULTIPLE_FILES) {
            if (data.getData() != null) {
                addReferencedFile(data.getData());
            } else if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    addReferencedFile(data.getClipData().getItemAt(i).getUri());
                }
                Toast.makeText(this, count + " arquivo(s) importado(s)", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_IMPORT_FOLDER) {
            Uri treeUri = data.getData();
            if (treeUri == null) return;
            try {
                getContentResolver().takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            addReferencedFolder(treeUri);
        }
    }

    private void addReferencedFile(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        if (resourceDao.existsUri(uri.toString(), UserResource.TYPE_REFERENCED_FILE)) {
            Toast.makeText(this, "Arquivo já está na biblioteca", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = extractFileName(uri);
        String mime = getContentResolver().getType(uri);
        if (mime == null) mime = "application/octet-stream";

        UserResource res = new UserResource();
        res.setTitle(title);
        res.setUri(uri.toString());
        res.setMimeType(mime);
        res.setSize(extractFileSize(uri));
        res.setType(UserResource.TYPE_REFERENCED_FILE);
        res.setFolderId(-1);
        res.setParentId(0);
        res.setCreatedAt(System.currentTimeMillis());
        resourceDao.insert(res);
        refreshResources();
        Toast.makeText(this, "Arquivo importado", Toast.LENGTH_SHORT).show();
    }

    private String extractFileName(Uri uri) {
        String name = null;
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception e) {
            // ignorar falha de leitura de nome, usar fallback
        }
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
        } catch (Exception e) {
            // ignorar falha de leitura de tamanho, usar zero
        }
        return size;
    }

    private void addReferencedFolder(Uri uri) {
        if (resourceDao.existsUri(uri.toString(), UserResource.TYPE_REFERENCED_FOLDER)) {
            Toast.makeText(this, "Pasta já está na biblioteca", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Importando pasta...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            final long id = resourceDao.importFolderTree(getContentResolver(), uri, 0);
            runOnUiThread(() -> {
                if (id > 0) {
                    refreshResources();
                    Toast.makeText(this, "Pasta importada com subpastas", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Erro ao importar pasta", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}