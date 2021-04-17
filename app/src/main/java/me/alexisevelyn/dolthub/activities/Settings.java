package me.alexisevelyn.dolthub.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import me.alexisevelyn.dolthub.R;
import me.alexisevelyn.dolthub.fragments.SettingsFragment;

public class Settings extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // TODO: Key Cannot Be Null
        getSupportFragmentManager().beginTransaction().replace(R.id.settings_preferences,
                new SettingsFragment()).commit();
    }
}
