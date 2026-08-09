package com.biblia.estudo.utils;

import android.app.Activity;
import android.app.AlertDialog;

/**
 * Menu "+ IMPORTAR" compartilhado entre Home e Meus Recursos.
 * As opções executam os runnables fornecidos por cada tela.
 */
public class ResourceImportMenu {

    public static void show(Activity activity,
                            final Runnable importFile,
                            final Runnable importFolder,
                            final Runnable createFolder) {
        String[] options = {
                "\uD83D\uDCC4 Importar Arquivo",
                "\uD83D\uDCC2 Importar Pasta",
                "\uD83D\uDCC1 Criar Pasta"
        };
        new AlertDialog.Builder(activity)
                .setTitle("+ IMPORTAR")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: importFile.run(); break;
                        case 1: importFolder.run(); break;
                        case 2: createFolder.run(); break;
                    }
                })
                .show();
    }
}
