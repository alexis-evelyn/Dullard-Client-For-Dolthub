package me.alexisevelyn.dullard.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import me.alexisevelyn.dullard.R;
import me.alexisevelyn.dullard.fragments.SettingsFragment;

// https://developer.android.com/guide/topics/ui/settings
public class Settings extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.settings_fragment, new SettingsFragment());
        fragmentTransaction.commit();
    }
}
