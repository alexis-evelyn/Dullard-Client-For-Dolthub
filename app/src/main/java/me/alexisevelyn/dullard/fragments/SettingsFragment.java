package me.alexisevelyn.dullard.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import me.alexisevelyn.dullard.R;
import me.alexisevelyn.dullard.utilities.HelperMethods;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String tagName = "DullardSettings";

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

        SwitchPreference root_preferences = findPreference("root_preferences");
        assert root_preferences != null; // This should never be null as it's builtin to the app

        root_preferences.setOnPreferenceChangeListener((preference, desiresRoot) -> {
            Log.d(tagName, "User Desires ROOT: " + desiresRoot);

            // If the user does not want root, we turn it off, no questions asked (and no warning popped up)
            if (! (boolean) desiresRoot)
                return true;

            confirmRootPermission(root_preferences);
            return false;
        });
    }

    private void confirmRootPermission(SwitchPreference root_preferences) {
        Context context = getContext();

        // If anything goes wrong, we deny root permission
        if (context == null)
            return;

        // We use a custom dialog with overlay detection on it to prevent third party apps from trying to interfere with the user
        ConfirmRootDialogFragment confirmRootDialogFragment = new ConfirmRootDialogFragment();
        confirmRootDialogFragment.setPositiveButtonCallback((view) -> allowRootPermission(confirmRootDialogFragment, context, root_preferences));
        confirmRootDialogFragment.setNegativeButtonCallback((view) -> denyRootPermission(confirmRootDialogFragment, context, root_preferences));
        confirmRootDialogFragment.show(getParentFragmentManager(), "ConfirmRoot");
    }

    private void allowRootPermission(ConfirmRootDialogFragment dialogFragment, Context context, SwitchPreference root_preferences) {
        root_preferences.setChecked(true);
        Toast.makeText(context, "Allowed", Toast.LENGTH_SHORT).show();

        dialogFragment.dismiss();
    }

    private void denyRootPermission(ConfirmRootDialogFragment dialogFragment, Context context, SwitchPreference root_preferences) {
        root_preferences.setChecked(false); // Technically not needed since we start with an off state and ignore the switch check to begin with
        Toast.makeText(context, "Denied", Toast.LENGTH_SHORT).show();

        dialogFragment.dismiss();
    }
}