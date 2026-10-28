package com.biblia.estudo.ui.library;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.biblia.estudo.R;
import com.biblia.estudo.app.BibliaApplication;

public class SplashActivity extends Activity {

    private ProgressBar progressBar;
    private TextView statusText;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Material_NoActionBar);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.splashProgress);
        statusText = findViewById(R.id.splashStatus);

        new Thread(new InitRunnable()).start();
    }

    private class InitRunnable implements Runnable {
        @Override
        public void run() {
            try {
                publishProgress(25);
                Thread.sleep(300);

                publishProgress(50);
                BibliaApplication.getThemeManager();

                publishProgress(75);
                SQLiteDatabase db = BibliaApplication.getDatabaseManager().getBibleDatabase();
                if (db == null) {
                    throw new RuntimeException("Banco de dados não pôde ser aberto");
                }
                db.getVersion();

                publishProgress(100);
                Thread.sleep(400);

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            } catch (Exception e) {
                final String erro = e.getMessage();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (statusText != null) {
                            statusText.setText("Erro ao iniciar: " + (erro != null ? erro : "desconhecido"));
                        }
                        Toast.makeText(SplashActivity.this, "Falha ao inicializar o aplicativo", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    private void publishProgress(final int value) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (statusText != null) {
                    if (value == 25) statusText.setText("Preparando aplicativo...");
                    else if (value == 50) statusText.setText("Carregando recursos...");
                    else if (value == 75) statusText.setText("Abrindo banco de dados...");
                    else if (value == 100) statusText.setText("Pronto!");
                }
                if (progressBar != null) progressBar.setProgress(value);
            }
        });
    }
}
