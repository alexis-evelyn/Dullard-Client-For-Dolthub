package me.alexisevelyn.dolthub.fragments;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import me.alexisevelyn.dolthub.R;

public class LegalFragment extends PreferenceFragmentCompat {
    private static String tagName = "DoltLegal";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_legal, rootKey);
    }
}
