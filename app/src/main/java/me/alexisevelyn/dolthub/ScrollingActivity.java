package me.alexisevelyn.dolthub;

import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ScrollingActivity extends AppCompatActivity {
    private static String tagName = "DoltScrolling";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        populateRepos();
    }

    private void populateRepos() {
        Api api = new Api();
        JSONArray repos = api.listRepos(getApplicationContext());

        // For Populating
        LinearLayout repoView = findViewById(R.id.repos);
        repoView.removeAllViews(); // Clear Existing Views

        // See https://stackoverflow.com/a/46261385/6828099 as to why a For Each Loop Doesn't Work
        int totalRepos = repos.length();
        for(int i = 0; i < totalRepos; i++) {
            try {
                JSONObject repo = (JSONObject) repos.get(i);
                // Log.d(tagName, repo.toString());

                String repoName = repo.get("repoName").toString();
                String ownerName = repo.get("ownerName").toString();
                String description = repo.get("description").toString();
                int forks = (int) repo.get("forkCount");
                int stars = (int) repo.get("starCount");

                String display = String.format("%s/%s - %s", ownerName, repoName, description);

                Button repoItem = new Button(getApplicationContext());
                repoItem.setText(display);
//                repoItem.setTextColor(Color.RED);

                repoView.addView(repoItem);

                Log.d(tagName, display);
            } catch (JSONException e) {
                Log.e(tagName, "Failed To Read Repo Numbered: " + i);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_version_test) {
            Cli cli = new Cli();
            String version = cli.getVersion(getApplicationContext());
            View view = getWindow().getDecorView().findViewById(android.R.id.content);

            Snackbar.make(view, version, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            return true;
        } else if (id == R.id.action_clone_repo_test) {
            Cli cli = new Cli();
            cli.cloneRepo(getApplicationContext());
            View view = getWindow().getDecorView().findViewById(android.R.id.content);

//            Snackbar.make(view, version, Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();

            return true;
        } else if (id == R.id.action_read_rows_test) {
            Cli cli = new Cli();
            String rows = cli.readRows(getApplicationContext());
            View view = getWindow().getDecorView().findViewById(android.R.id.content);

            Snackbar.make(view, rows, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}