package me.alexisevelyn.dolthub;

import android.content.Intent;
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ScrollingActivity extends AppCompatActivity {
    private static String tagName = "DoltScrolling";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());

        retrieveAndPopulateRepos();
    }

    private void retrieveAndPopulateRepos() {
        // For those interested, I have to do network activity in
        // a background thread and UI activity in the main thread.
        // The main thread is otherwise known as the UI Thread.

        // API Handler - Handles Cached and Live API Responses
        Api api = new Api(getApplicationContext());

        // Retrieve Cached Repos If Any
        // We do this on the main thread as the cached data is pulled from the same partition as the app
        //   and instead of waiting 220+ ms for the AtomicReference to propagate threads, we just assume that the
        //   cached data will pull as fast as the app can (which for most phones is far faster than the 220+ ms of propagation waiting).
        JSONArray cached = api.retrieveCachedRepos();
        populateRepos(cached);

        // For Thread Safe Propagating Variables Across Threads
        AtomicReference<JSONArray> repos = new AtomicReference<>();

        Runnable updateUI = () -> {
            populateRepos(repos.get());
        };

        Runnable reposRunnable = () -> {
            // Attempt Live Update
            repos.compareAndSet(repos.get(), retrieveRepos(api));
            runOnUiThread(updateUI);
        };

        Thread reposThread = new Thread(reposRunnable);
        reposThread.start();
    }

    private JSONArray retrieveRepos(Api api) {
        return api.listRepos();
    }

    private void populateRepos(JSONArray repos) {
        if(repos == null)
            return;

        // TODO: Determine How User Wants Repos Sorted
        repos = HelperMethods.sortReposBySize(repos);

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

                String repoSize = "N/A";
                try {
                    String rawRepoSize = repo.getString("size");
                    repoSize = HelperMethods.humanReadableByteCountSI(Long.parseLong(rawRepoSize));
                } catch (NumberFormatException e) {
                    String rawRepoSize = repo.getString("size");
                    Log.e(tagName, String.format("Repo: %s/%s contains an invalid size `%s`!!!", ownerName, repoName, rawRepoSize));
                }

                String display = String.format("%s/%s - %s - Size %s", ownerName, repoName, description, repoSize);

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
            Cli cli = new Cli(getApplicationContext());
            backgroundReturnValue.set(HelperMethods.strip(cli.getVersion()));

            runOnUiThread(updateUI);
        };

        Thread backgroundThread = new Thread(backgroundRunnable);
        backgroundThread.start();
    }

    private void cloneRepo(View view) {
        AtomicReference<Object> backgroundReturnValue = new AtomicReference<>();

        Runnable updateUI = () -> Snackbar.make(view, (String) backgroundReturnValue.get(), Snackbar.LENGTH_LONG).setAction("Action", null).show();

        Runnable backgroundRunnable = () -> {
            Cli cli = new Cli(getApplicationContext());
            cli.cloneRepo();

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
            Cli cli = new Cli(getApplicationContext());
            backgroundReturnValue.set(HelperMethods.strip(cli.readRows()));

            runOnUiThread(updateUI);
        };

        Thread backgroundThread = new Thread(backgroundRunnable);
        backgroundThread.start();
    }
}