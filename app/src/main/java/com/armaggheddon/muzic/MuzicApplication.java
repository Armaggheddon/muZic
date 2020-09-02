package com.armaggheddon.muzic;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

/**
 * Extending Application class allows to keep the application state even on configuration changes,
 * for example for theme changes.
 * This implementation only handles theme change
 */

public class MuzicApplication extends Application {

    /* Static references to the values stored in the shared preferences for theme options */
    private static final String THEME_LIGHT_ENTRY = "light";
    private static final String THEME_DARK_ENTRY = "dark";
    private static final String THEME_DEFAULT_ENTRY = "default";
    public static boolean IS_FIRST_TIME = true;

    public void onCreate(){
        super.onCreate();

        /* Get the preference for the theme */
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = sharedPreferences.getString( getString(R.string.theme_key_shared_prefs), THEME_DEFAULT_ENTRY);

        /* Apply the theme */
        applyTheme(themePref);
        /*
        SharedPreferences.Editor editor = getSharedPreferences("display_tutorial", MODE_PRIVATE).edit();
        editor.putBoolean("display_tutorial", true);
        editor.commit();
         */
        SharedPreferences preferences = getSharedPreferences(getString(R.string.display_tutorial_option), MODE_PRIVATE);
        IS_FIRST_TIME = preferences.getBoolean(getString(R.string.display_tutorial_option), true);
    }

    public static void showAgain(boolean NEW_IS_FIRST_TIME){
        IS_FIRST_TIME = NEW_IS_FIRST_TIME;
    }

    /**
     * Sets the theme based on {@param themePref} value using AppCompatDelegate. If the option is
     * {@value MuzicApplication#THEME_DEFAULT_ENTRY} then the option is different based on the
     * Android version the device has.
     * @param themePref
     *                  The value stored in the SharedPreferences, is one of:
     *                  -{@value MuzicApplication#THEME_LIGHT_ENTRY}
     *                  -{@value MuzicApplication#THEME_DARK_ENTRY}
     *                  -{@value MuzicApplication#THEME_DEFAULT_ENTRY}
     */
    public static void applyTheme(String themePref){

        switch (themePref){
            case THEME_LIGHT_ENTRY:

                /* Disable the night mode and set the theme to be always light */
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK_ENTRY:

                /* Set the night mode to be always enabled */
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_DEFAULT_ENTRY:

                /* Based on the version */
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)

                    /*
                    From Android Q the dark mode is supported system wide so set the option to
                    follow the current system theme
                    */
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                else
                    /*
                    On previous versions, the dark theme was implemented in a different way,
                    so set the theme to follow the current battery status, becoming dark on low
                    battery (when the battery save option is enabled)
                     */
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
        }
    }
}
