package me.alexisevelyn.dullard.fragments;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import me.alexisevelyn.dullard.R;
import me.alexisevelyn.dullard.utilities.HelperMethods;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static String tagName = "DullardSettings";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        // Set Day/Night Mode From User's Choice
        ListPreference day_night_preferences = findPreference("themes_day_night_preferences");
        assert day_night_preferences != null; // This should never be null as it's builtin to the app

        day_night_preferences.setOnPreferenceChangeListener((preference, day_night_value) -> {
            Log.d(tagName, "Chosen Day/Night: " + day_night_value);

            // getActivity().getApplicationContext().setTheme(R.style.Theme_AppCompat);
            return HelperMethods.setDayNightMode((String) day_night_value);
        });
    }
}