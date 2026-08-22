package com.biblia.estudo.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.biblia.estudo.R;
import com.biblia.estudo.ui.crossref.ReferenceMapActivity;
import com.biblia.estudo.ui.dictionary.DictionaryActivity;
import com.biblia.estudo.ui.favorites.FavoritesActivity;
import com.biblia.estudo.ui.highlights.HighlightsActivity;
import com.biblia.estudo.ui.library.HomeActivity;
import com.biblia.estudo.ui.library.LibraryActivity;
import com.biblia.estudo.ui.notes.NotesActivity;
import com.biblia.estudo.ui.readingplan.ReadingPlanActivity;
import com.biblia.estudo.ui.search.SearchActivity;
import com.biblia.estudo.ui.settings.SettingsActivity;
import com.biblia.estudo.ui.study.TopicIndexActivity;

public class NavigationHelper {

    public static void setupBottomNav(Activity activity) {
        setupBottomNav(activity, -1);
    }

    public static void setupBottomNav(Activity activity, int activeNavId) {
        View navHome = activity.findViewById(R.id.navHome);
        View navBible = activity.findViewById(R.id.navBible);
        View navSearch = activity.findViewById(R.id.navSearch);
        View navFavorites = activity.findViewById(R.id.navFavorites);
        View navHighlights = activity.findViewById(R.id.navHighlights);
        View navMore = activity.findViewById(R.id.navMore);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                if (!(activity instanceof HomeActivity)) {
                    activity.startActivity(new Intent(activity, HomeActivity.class));
                }
            });
            if (activeNavId == R.id.navHome) applyActiveState(activity, navHome);
        }
        if (navBible != null) {
            navBible.setOnClickListener(v -> {
                if (!(activity instanceof LibraryActivity)) {
                    activity.startActivity(new Intent(activity, LibraryActivity.class));
                }
            });
            if (activeNavId == R.id.navBible) applyActiveState(activity, navBible);
        }
        if (navSearch != null) {
            navSearch.setOnClickListener(v -> {
                if (!(activity instanceof SearchActivity)) {
                    activity.startActivity(new Intent(activity, SearchActivity.class));
                }
            });
            if (activeNavId == R.id.navSearch) applyActiveState(activity, navSearch);
        }
        if (navFavorites != null) {
            navFavorites.setOnClickListener(v -> {
                if (!(activity instanceof FavoritesActivity)) {
                    activity.startActivity(new Intent(activity, FavoritesActivity.class));
                }
            });
            if (activeNavId == R.id.navFavorites) applyActiveState(activity, navFavorites);
        }
        if (navHighlights != null) {
            navHighlights.setOnClickListener(v -> {
                if (!(activity instanceof HighlightsActivity)) {
                    activity.startActivity(new Intent(activity, HighlightsActivity.class));
                }
            });
            if (activeNavId == R.id.navHighlights) applyActiveState(activity, navHighlights);
        }
        if (navMore != null) {
            navMore.setOnClickListener(v -> {
                showMoreMenu(activity);
            });
            if (activeNavId == R.id.navMore) applyActiveState(activity, navMore);
        }
    }

    private static void applyActiveState(Activity activity, View navItem) {
        navItem.setBackgroundResource(R.drawable.nav_item_active);
    }

    private static void showMoreMenu(Activity activity) {
        String[] items = {"Referências Cruzadas", "Dicionário", "Índice Temático", "Plano de Leitura", "Progresso de Leitura", "Notas", "Configurações"};
        final Intent[] intents = {
                new Intent(activity, ReferenceMapActivity.class),
                new Intent(activity, DictionaryActivity.class),
                new Intent(activity, TopicIndexActivity.class),
                new Intent(activity, ReadingPlanActivity.class),
                new Intent(activity, ReadingPlanActivity.class),
                new Intent(activity, NotesActivity.class),
                new Intent(activity, SettingsActivity.class)
        };
        new android.app.AlertDialog.Builder(activity)
                .setTitle("Mais Recursos")
                .setItems(items, (dialog, which) -> activity.startActivity(intents[which]))
                .show();
    }

    public static void setupBackButton(Activity activity) {
        View btnBack = activity.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> activity.finish());
        }
    }
}