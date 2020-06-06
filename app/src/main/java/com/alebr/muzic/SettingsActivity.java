package com.alebr.muzic;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

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
        }
    }
}