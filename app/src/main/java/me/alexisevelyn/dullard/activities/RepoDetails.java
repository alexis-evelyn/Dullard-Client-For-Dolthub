package me.alexisevelyn.dullard.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import me.alexisevelyn.dullard.R;
import me.alexisevelyn.dullard.utilities.HelperMethods;
import me.alexisevelyn.dullard.utilities.RepoDetailsHelper;

public class RepoDetails extends AppCompatActivity {
    private static final String tagName = "DullardRepoDetails";
    private RepoDetailsHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HelperMethods.loadDayNightPreferences(getApplicationContext());
        setContentView(R.layout.activity_repo_details);

        this.helper = new RepoDetailsHelper(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_repo_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        View view = getWindow().getDecorView().findViewById(android.R.id.content);

        if (id == R.id.action_settings) {
            HelperMethods.openSettings(view);

            return true;
        } else if (id == R.id.action_share_repo) {
            this.helper.shareRepo();
            return true;
        } else if (id == R.id.action_clone_repo) {
            this.helper.cloneRepo(view);
            return true;
        } else if (id == R.id.action_start_sql_server) {
            this.helper.startSQLServer(view);
            return true;
        } else if (id == R.id.action_stop_sql_server) {
            this.helper.stopSQLServer(view);
            return true;
        }

        return false;
    }
}