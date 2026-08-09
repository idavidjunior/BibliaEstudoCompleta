package com.biblia.estudo.utils;

import android.net.Uri;

/**
 * Identifica de onde vem o arquivo/pasta referenciado:
 * armazenamento interno do aparelho, cartão SD ou nuvem.
 */
public class StorageOrigin {

    public static final String DEVICE = "Dispositivo";
    public static final String SD_CARD = "SD";
    public static final String CLOUD = "Nuvem";

    /** Retorna o rótulo de origem (com emoji) ou null se não detectável. */
    public static String label(Uri uri) {
        String origin = resolve(uri);
        if (origin == null) return null;
        if (origin.equals(DEVICE)) return "\uD83D\uDCF1 " + DEVICE;
        if (origin.equals(SD_CARD)) return "\uD83D\uDCBE " + SD_CARD;
        return "\u2601\uFE0F " + CLOUD;
    }

    /** Retorna DEVICE, SD_CARD, CLOUD ou null. */
    public static String resolve(Uri uri) {
        if (uri == null) return null;
        String authority = uri.getAuthority();
        if (authority == null) return null;
        String a = authority.toLowerCase();

        // Provedor de armazenamento externo: "primary" = aparelho, demais = SD/USB
        if (a.equals("com.android.externalstorage.documents")) {
            String path = uri.getPath();
            if (path != null && (path.contains(":primary") || path.contains("/primary/"))) {
                return DEVICE;
            }
            return SD_CARD;
        }

        // Provedores locais do Android
        if (a.equals("com.android.providers.downloads.documents")
                || a.equals("com.android.providers.media.documents")
                || a.equals("com.android.providers.media")
                || a.equals("media")
                || a.equals("com.android.providers.downloads")) {
            return DEVICE;
        }

        // Nuvens conhecidas
        if (a.contains("google.android.apps.docs") || a.contains("apps.docs")
                || a.contains("docs.google") || a.contains("google.docs")
                || a.contains("skydrive") || a.contains("onedrive")
                || a.contains("dropbox") || a.contains("box.")
                || a.contains("mega") || a.contains("icloud") || a.contains("clouddocs")
                || a.contains("android.apps.photos")
                || a.contains("yandex") || a.contains("gdrive")) {
            return CLOUD;
        }
        return null;
    }
}
