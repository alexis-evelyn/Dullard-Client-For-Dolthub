package me.alexisevelyn.dullard.fragments;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import me.alexisevelyn.dullard.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static String tagName = "DullardSettings";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);
    }
}
