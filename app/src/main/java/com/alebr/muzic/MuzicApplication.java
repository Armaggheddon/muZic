package com.alebr.muzic;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.preference.PreferenceManager;

import java.util.Arrays;

public class MuzicApplication extends Application {

    private static final String THEME_LIGHT_ENTRY = "light";
    private static final String THEME_DARK_ENTRY = "dark";
    private static final String THEME_DEFAULT_ENTRY = "default";

    public void onCreate(){
        super.onCreate();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = sharedPreferences.getString( getString(R.string.theme_key_shared_prefs), THEME_DEFAULT_ENTRY);
        applyTheme(themePref);
    }

    public static void applyTheme(String themePref){

        switch (themePref){
            case THEME_LIGHT_ENTRY:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK_ENTRY:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_DEFAULT_ENTRY:
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                else
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
        }
    }
}
