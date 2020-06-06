package com.alebr.muzic;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Arrays;

public class SettingsActivity extends AppCompatActivity {

    private static final String ARTIST_SHORTCUT_ID = "shortcut_artist_id";
    private static final String SONG_SHORTCUT_ID = "shortcut_song_id";
    private static final String QUEUE_SHORTCUT_ID = "shortcut_queue_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        MaterialToolbar mToolbar = findViewById(R.id.toolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            String[] themeValues = null;

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                themeValues = getResources().getStringArray(R.array.theme_q_values);
            }else {
                themeValues = getResources().getStringArray(R.array.theme_before_q_values);
            }
            ListPreference themePreferences = findPreference(getString(R.string.theme_key_shared_prefs));
            if(themePreferences != null) {
                themePreferences.setEntries(themeValues);
                themePreferences.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String themeOption = (String) newValue;
                        MuzicApplication.applyTheme(themeOption);
                        return true;
                    }
                });
            }

            SwitchPreference appShortcutsPreference = findPreference(getString(R.string.launcher_shortcut_key_shared_prefs));

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1){
                appShortcutsPreference.setEnabled(false);
                appShortcutsPreference.setChecked(false);
                appShortcutsPreference.setSummary(getString(R.string.launcher_shortcut_summary_not_supported));
            }else {
                if (appShortcutsPreference != null) {
                    appShortcutsPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {

                            boolean setShortcuts = (boolean) newValue;

                            if(setShortcuts){
                                setShortcuts();
                            } else {
                                removeShortcuts();
                            }
                            return true;
                        }
                    });
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.N_MR1)
        private void setShortcuts(){
            ShortcutManager manager = getActivity().getSystemService(ShortcutManager.class);

            if(manager.getDynamicShortcuts().size() != 0)
                manager.removeAllDynamicShortcuts();

            Intent artistsIntent = new Intent( getActivity(), MainActivity.class);
            artistsIntent.setAction(Intent.ACTION_VIEW);
            artistsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            artistsIntent.putExtra(MainActivity.LAUNCHER_SHORTCUTS_INTENT_KEY, ListFragment.ARTIST_FRAGMENT_TAG);

            Intent songsIntent = new Intent( getActivity(), MainActivity.class);
            songsIntent.setAction(Intent.ACTION_VIEW);
            songsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            songsIntent.putExtra(MainActivity.LAUNCHER_SHORTCUTS_INTENT_KEY, ListFragment.SONG_FRAGMENT_TAG);

            Intent queueIntent = new Intent( getActivity(), MainActivity.class);
            queueIntent.setAction(Intent.ACTION_VIEW);
            queueIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            queueIntent.putExtra(MainActivity.LAUNCHER_SHORTCUTS_INTENT_KEY, QueueFragment.QUEUE_FRAGMENT_TAG);

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

            //The items are showed from top to bottom as "artistInfo", "songInfo" and "queueInfo"
            manager.setDynamicShortcuts(Arrays.asList(queueInfo, songInfo, artistInfo));
        }

        @RequiresApi(Build.VERSION_CODES.N_MR1)
        private void removeShortcuts(){
            ShortcutManager manager = getActivity().getSystemService(ShortcutManager.class);
            if(manager.getDynamicShortcuts().size() != 0){
                manager.removeAllDynamicShortcuts();
            }
        }

    }
}