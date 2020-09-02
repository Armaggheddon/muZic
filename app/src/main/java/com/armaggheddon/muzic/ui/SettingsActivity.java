package com.armaggheddon.muzic.ui;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.armaggheddon.muzic.MuzicApplication;
import com.armaggheddon.muzic.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.Arrays;


/**
 * Handles the settings showed to the user in {@link R.layout#settings_activity} and saves them
 */
public class SettingsActivity extends AppCompatActivity {

    /* IDs of the shortcuts published */
    private static final String ARTIST_SHORTCUT_ID = "shortcut_artist_id";
    private static final String SONG_SHORTCUT_ID = "shortcut_song_id";
    private static final String QUEUE_SHORTCUT_ID = "shortcut_queue_id";

    /* If TRUE the device has built-in equalizer, else it does not have one */
    private static boolean IS_EQ_AVAILABLE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        /* Set up the back click listener */
        MaterialToolbar mToolbar = findViewById(R.id.toolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* Just close the SettingsActivity there are no operations to do */
                finish();
            }
        });

        /* Check if the equalizer is available for the device since not avery device has one */
        if( new Intent( AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).resolveActivity(getPackageManager()) != null)
            IS_EQ_AVAILABLE = true;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            String[] themeValues;

            /* Load the appropriate theme settings based on the device OS version */
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                themeValues = getResources().getStringArray(R.array.theme_q_values);
            }else {
                themeValues = getResources().getStringArray(R.array.theme_before_q_values);
            }

            /* Get the preference for the theme */
            ListPreference themePreferences = findPreference(getString(R.string.theme_key_shared_prefs));
            if(themePreferences != null) {

                /* Update the values being shown to the user */
                themePreferences.setEntries(themeValues);
                themePreferences.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {

                        /* When an theme entry is selected update the theme accordingly */
                        String themeOption = (String) newValue;
                        MuzicApplication.applyTheme(themeOption);

                        /* Return true telling that the event was handled */
                        return true;
                    }
                });
            }

            /*
            If IS_EQ_AVAILABLE is false then hide the setting to launch the equalizer, since the
            click event will result in an app crash
             */
            Preference equalizer = findPreference(getString(R.string.shared_prefs_equalizer_option));
            if (!IS_EQ_AVAILABLE && equalizer != null)
                equalizer.setVisible(false);
            /* If is available then open the default equalizer */
            if(IS_EQ_AVAILABLE && equalizer != null){
                equalizer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                        startActivityForResult(intent, 10);
                        return true;
                    }
                });
            }

            /* Get the preference for the launcher shortcut */
            SwitchPreference appShortcutsPreference = findPreference(getString(R.string.launcher_shortcut_key_shared_prefs));
            if (appShortcutsPreference != null) {
                /* Launcher shortcuts are supported only on Android version N (25) or later */
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {

                    /* The device does not support launcher shortcuts so disable the setting option */
                    appShortcutsPreference.setEnabled(false);

                    /* Ensure that the value is set to false */
                    appShortcutsPreference.setChecked(false);

                    /* Set a description about why the option is not enabled */
                    appShortcutsPreference.setSummary(getString(R.string.launcher_shortcut_summary_not_supported));
                } else {

                    appShortcutsPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {

                            boolean setShortcuts = (boolean) newValue;

                            /* If the value is set to true publish the shortcuts to the system */
                            if (setShortcuts) {
                                setShortcuts();
                            }

                            /* Else remove the shortcuts */
                            else {
                                removeShortcuts();
                            }

                            /* Return true telling that the event was handled */
                            return true;
                        }
                    });
                }
            }
        }

        /**
         * Builds the intent that allow the shortcut to comunicate to the activity being launched
         * which is {@link MainActivity} this method requires {@value Build.VERSION_CODES#N_MR1}
         * or later to work
         */
        @RequiresApi(Build.VERSION_CODES.N_MR1)
        private void setShortcuts(){

            /* Get a ShortcutManager to set and later publish the shortcuts */
            ShortcutManager manager = getActivity().getSystemService(ShortcutManager.class);

            /* Remove any published shortcuts to avoid adding the current ones to the existing ones */
            if(manager.getDynamicShortcuts().size() != 0)
                manager.removeAllDynamicShortcuts();

            /*
            Build the intents for the shortcuts to open the related fragment when the app is opened.
            The same applies for songsIntent and queueIntent
            */
            Intent artistsIntent = new Intent( getActivity(), MainActivity.class);
            artistsIntent.setAction(Intent.ACTION_VIEW);

            /* Flag as SINGLE_TOP to avoid multiple instances of the same activity being launched */
            artistsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            /* Set the intent to send to MainActivity onCreate method */
            artistsIntent.putExtra(MainActivity.LAUNCHER_SHORTCUTS_INTENT_KEY, ListFragment.ARTIST_FRAGMENT_TAG);

            Intent songsIntent = new Intent( getActivity(), MainActivity.class);
            songsIntent.setAction(Intent.ACTION_VIEW);
            songsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            songsIntent.putExtra(MainActivity.LAUNCHER_SHORTCUTS_INTENT_KEY, ListFragment.SONG_FRAGMENT_TAG);

            Intent queueIntent = new Intent( getActivity(), MainActivity.class);
            queueIntent.setAction(Intent.ACTION_VIEW);
            queueIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            queueIntent.putExtra(MainActivity.LAUNCHER_SHORTCUTS_INTENT_KEY, QueueFragment.QUEUE_FRAGMENT_TAG);

            /* Build the ShortcutInfo object that represent the shortcut */
            ShortcutInfo artistInfo = new ShortcutInfo.Builder(getContext(), ARTIST_SHORTCUT_ID)
                    .setShortLabel(getString(R.string.artists_title))
                    .setLongLabel(getString(R.string.artists_title))
                    .setIcon(Icon.createWithResource( getContext(), R.drawable.ic_shortcut_artist))
                    .setIntent(artistsIntent)
                    .build();

            ShortcutInfo songInfo = new ShortcutInfo.Builder(getContext(), SONG_SHORTCUT_ID)
                    .setShortLabel(getString(R.string.songs_title))
                    .setLongLabel(getString(R.string.songs_title))
                    .setIcon(Icon.createWithResource( getContext(), R.drawable.ic_shortcut_song))
                    .setIntent(songsIntent)
                    .build();

            ShortcutInfo queueInfo = new ShortcutInfo.Builder(getContext(), QUEUE_SHORTCUT_ID)
                    .setShortLabel(getString(R.string.queue_title))
                    .setLongLabel(getString(R.string.queue_title))
                    .setIcon(Icon.createWithResource( getContext(), R.drawable.ic_shortcut_queue))
                    .setIntent(queueIntent)
                    .build();

            /* The items are showed from top to bottom as "artistInfo", "songInfo" and "queueInfo" */
            manager.setDynamicShortcuts(Arrays.asList(queueInfo, songInfo, artistInfo));
        }

        /* Removes any available shortcuts from the launcher icon */
        @RequiresApi(Build.VERSION_CODES.N_MR1)
        private void removeShortcuts(){
            ShortcutManager manager = getActivity().getSystemService(ShortcutManager.class);

            /* If the number of shortcuts available is not 0 then remove the shortcuts */
            if(manager.getDynamicShortcuts().size() != 0){
                manager.removeAllDynamicShortcuts();
            }
        }

    }
}