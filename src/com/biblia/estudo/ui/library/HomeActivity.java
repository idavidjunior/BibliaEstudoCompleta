package com.biblia.estudo.ui.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
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
import com.biblia.estudo.ui.notes.NotesActivity;
import com.biblia.estudo.ui.resources.ResourcesActivity;
import com.biblia.estudo.utils.NavigationHelper;

import java.util.List;

public class HomeActivity extends Activity {

    private static final int REQUEST_IMPORT_FILE = 1001;
    private static final int REQUEST_IMPORT_FOLDER = 1002;
    private static final int REQUEST_IMPORT_MULTIPLE_FILES = 1003;

    private TextView lastReadingRef, lastReadingText;
    private Button btnContinue, btnStart;
    private TextView verseOfDayText, verseOfDayRef;
    private android.widget.FrameLayout resourcesSection, notesSection;
    private ListView resourceList;
    private View emptyResources;
    private TextView notesPreview;

    private ReadingProgressDao progressDao;
    private BookDao bookDao;
    private VerseDao verseDao;
    private UserResourceDao resourceDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BibliaApplication.getThemeManager().applyTheme(this);
        setContentView(R.layout.activity_home);

        SQLiteDatabase db = BibliaApplication.getDatabaseManager().getBibleDatabase();
        progressDao = new ReadingProgressDao(db);
        bookDao = new BookDao(db);
        verseDao = new VerseDao(db);
        resourceDao = new UserResourceDao(db);

        lastReadingRef = findViewById(R.id.lastReadingRef);
        lastReadingText = findViewById(R.id.lastReadingText);
        btnContinue = findViewById(R.id.btnContinueReading);
        btnStart = findViewById(R.id.btnStartReading);
        verseOfDayText = findViewById(R.id.verseOfDayText);
        verseOfDayRef = findViewById(R.id.verseOfDayRef);
        resourcesSection = findViewById(R.id.resourcesSection);
        notesSection = findViewById(R.id.notesSection);
        resourceList = findViewById(R.id.resourceList);
        emptyResources = findViewById(R.id.emptyResources);
        notesPreview = findViewById(R.id.notesPreview);

        NavigationHelper.setupBottomNav(this);
        setupLastReading();
        setupVerseOfDay();
        setupButtons();
        setupResources();
        setupNotes();

        findViewById(R.id.btnImportFile).setOnClickListener(v -> importFile());
        findViewById(R.id.btnImportFolder).setOnClickListener(v -> importFolder());
        findViewById(R.id.btnCreateFolder).setOnClickListener(v -> createFolder());
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
                        last.getBookId(), last.getChapter(), last.getVerse(), last.getVerse());
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
        android.database.Cursor c = db.rawQuery(
                "SELECT v.text, v.chapter, v.verse_number, b.name FROM verses v " +
                        "JOIN books b ON v.book_id = b._id ORDER BY RANDOM() LIMIT 1", null);
        if (c != null && c.moveToFirst()) {
            String text = c.getString(0);
            int chapter = c.getInt(1);
            int verse = c.getInt(2);
            String bookName = c.getString(3);
            verseOfDayText.setText("\"" + text + "\"");
            verseOfDayRef.setText(bookName + " " + chapter + ":" + verse);
            c.close();
        }
    }

    private void setupButtons() {
        btnStart.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, LibraryActivity.class));
        });
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
            resourceDao.deleteById(res.getId());
            refreshResources();
            Toast.makeText(this, "Removido: " + res.getTitle(), Toast.LENGTH_SHORT).show();
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
            resourceList.setAdapter(new com.biblia.estudo.ui.library.ResourceListAdapter(this, resources));
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

    private void importFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_IMPORT_MULTIPLE_FILES);
    }

    private void importFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_IMPORT_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQUEST_IMPORT_MULTIPLE_FILES) {
            if (data.getData() != null) {
                // Single file selected
                Uri uri = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                addResource(uri);
            } else if (data.getClipData() != null) {
                // Multiple files selected
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    addResource(uri);
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
        res.setCreatedAt(System.currentTimeMillis());
        resourceDao.insert(res);
        refreshResources();
        Toast.makeText(this, "Importado: " + title, Toast.LENGTH_SHORT).show();
    }

    private void importFolderContents(Uri treeUri) {
        importFolderContentsRecursive(treeUri, "");
    }

    private void importFolderContentsRecursive(Uri treeUri, String parentPath) {
        try {
            android.database.Cursor c = getContentResolver().query(
                    android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,
                            android.provider.DocumentsContract.getTreeDocumentId(treeUri)),
                    new String[]{android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
                            android.provider.DocumentsContract.Document.COLUMN_SIZE},
                    null, null, null);
            if (c != null) {
                int count = 0;
                while (c.moveToNext()) {
                    String mime = c.getString(2);
                    String docId = c.getString(0);
                    String name = c.getString(1);
                    long size = c.getLong(3);

                    Uri childUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);

                    if (mime != null && mime.contains("vnd.android.document/directory")) {
                        // It's a subdirectory - recurse
                        String newPath = parentPath.isEmpty() ? name : parentPath + "/" + name;
                        importFolderContentsRecursive(childUri, newPath);
                    } else {
                        // It's a file - import with path
                        UserResource res = new UserResource();
                        String fullTitle = parentPath.isEmpty() ? name : parentPath + "/" + name;
                        res.setTitle(fullTitle);
                        res.setUri(childUri.toString());
                        res.setMimeType(mime);
                        res.setSize(size);
                        res.setCreatedAt(System.currentTimeMillis());
                        resourceDao.insert(res);
                        count++;
                    }
                }
                c.close();
                if (parentPath.isEmpty()) {
                    refreshResources();
                    Toast.makeText(this, count + " arquivos importados da pasta", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao importar pasta", Toast.LENGTH_SHORT).show();
        }
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
