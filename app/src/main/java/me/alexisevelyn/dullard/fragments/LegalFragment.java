package me.alexisevelyn.dullard.fragments;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import me.alexisevelyn.dullard.R;

public class LegalFragment extends PreferenceFragmentCompat {
    private static String tagName = "DullardLegal";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_legal, rootKey);
    }
}
