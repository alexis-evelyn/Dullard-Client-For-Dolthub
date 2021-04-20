package me.alexisevelyn.dullard.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import me.alexisevelyn.dullard.R;
import me.alexisevelyn.dullard.utilities.DiscoverReposHelper;
import me.alexisevelyn.dullard.utilities.HelperMethods;

public class DiscoverRepos extends AppCompatActivity {
    private static final String tagName = "DullardScrolling";

    private DiscoverReposHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HelperMethods.loadDayNightPreferences(getApplicationContext());
        setContentView(R.layout.activity_discover_repos);

        this.helper = new DiscoverReposHelper(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_discover_repos, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        View view = getWindow().getDecorView().findViewById(android.R.id.content);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            HelperMethods.openSettings(view);

            return true;
        } else if (id == R.id.action_version_test) {
            this.helper.getVersion(view);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}