package me.alexisevelyn.dolthub.fragments;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import me.alexisevelyn.dolthub.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static String tagName = "DoltSettings";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // TODO: Key Cannot Be Null
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);
    }
}
