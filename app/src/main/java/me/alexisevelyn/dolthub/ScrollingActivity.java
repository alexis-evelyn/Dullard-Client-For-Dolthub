package me.alexisevelyn.dolthub;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

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

        retrieveAndPopulateRepos();
    }

    private void retrieveAndPopulateRepos() {
        // For those interested, I have to do network activity in
        // a background thread and UI activity in the main thread.
        // The main thread is otherwise known as the UI Thread.

        AtomicReference<JSONArray> repos = new AtomicReference<>();

        Runnable updateUI = () -> {
            populateRepos(repos.get());
        };

        Runnable reposRunnable = () -> {
            repos.set(retrieveRepos());

            // Why one cannot just simply pass a variable is beyond me, but at least the Atomic Reference works
            runOnUiThread(updateUI);
        };

        Thread reposThread = new Thread(reposRunnable);
        reposThread.start();
    }

    private JSONArray retrieveRepos() {
        Api api = new Api();
        return api.listRepos(getApplicationContext());
    }

    private void populateRepos(JSONArray repos) {
        if(repos == null)
            return;

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

                description = !HelperMethods.strip(description).equals("") ? HelperMethods.strip(description) : getString(R.string.no_description);

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
        View view = getWindow().getDecorView().findViewById(android.R.id.content);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_version_test) {
            getVersion(view);

            return true;
        } else if (id == R.id.action_clone_repo_test) {
            cloneRepo(view);

            return true;
        } else if (id == R.id.action_read_rows_test) {
            readRows(view);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // You would think there would be a generic function to pass a ui thread and a background thread and have the background thread callback the ui thread for us.
    // TODO: Create that generic class!!!
    private void getVersion(View view) {
        AtomicReference<Object> backgroundReturnValue = new AtomicReference<>();

        Runnable updateUI = () -> Snackbar.make(view, (String) backgroundReturnValue.get(), Snackbar.LENGTH_LONG).setAction("Action", null).show();

        Runnable backgroundRunnable = () -> {
            Cli cli = new Cli();
            backgroundReturnValue.set(HelperMethods.strip(cli.getVersion(getApplicationContext())));

            runOnUiThread(updateUI);
        };

        Thread backgroundThread = new Thread(backgroundRunnable);
        backgroundThread.start();
    }

    private void cloneRepo(View view) {
        AtomicReference<Object> backgroundReturnValue = new AtomicReference<>();

        Runnable updateUI = () -> Snackbar.make(view, (String) backgroundReturnValue.get(), Snackbar.LENGTH_LONG).setAction("Action", null).show();

        Runnable backgroundRunnable = () -> {
            Cli cli = new Cli();
            cli.cloneRepo(getApplicationContext());

            String clonedRepo = getString(R.string.cloned_repo);
            backgroundReturnValue.set(clonedRepo);

            runOnUiThread(updateUI);
        };

        Thread backgroundThread = new Thread(backgroundRunnable);
        backgroundThread.start();
    }

    private void readRows(View view) {
        AtomicReference<Object> backgroundReturnValue = new AtomicReference<>();

        Runnable updateUI = () -> Snackbar.make(view, (String) backgroundReturnValue.get(), Snackbar.LENGTH_LONG).setAction("Action", null).show();

        Runnable backgroundRunnable = () -> {
            Cli cli = new Cli();
            backgroundReturnValue.set(HelperMethods.strip(cli.readRows(getApplicationContext())));

            runOnUiThread(updateUI);
        };

        Thread backgroundThread = new Thread(backgroundRunnable);
        backgroundThread.start();
    }
}