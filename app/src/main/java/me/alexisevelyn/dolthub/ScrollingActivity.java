package me.alexisevelyn.dolthub;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

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

    // For Reusing Same API Object
    private Api api = null;

    // The Private GraphQL API Often Repeats Repos We've Already Seen, So Sometimes We Have To Request More Data
    private int currentTries = 0;
    private int maxTries = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());

        // Set Up Scrolling Listener
        NestedScrollView nestedScrollView = (NestedScrollView) findViewById(R.id.repos_scroll_view);
        nestedScrollView.setOnScrollChangeListener(this::onScrollChanged);

//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show());

        // API Handler - Handles Cached and Live API Responses
        this.api = new Api(getApplicationContext());

        retrieveAndPopulateRepos();
    }

    private void retrieveAndPopulateRepos() {
        retrieveAndPopulateRepos(false);
    }

    private void retrieveAndPopulateRepos(boolean tryAgain) {
        // For those interested, I have to do network activity in
        // a background thread and UI activity in the main thread.
        // The main thread is otherwise known as the UI Thread.

        // Retrieve Cached Repos If Any
        // We do this on the main thread as the cached data is pulled from the same partition as the app
        //   and instead of waiting 220+ ms for the AtomicReference to propagate threads, we just assume that the
        //   cached data will pull as fast as the app can (which for most phones is far faster than the 220+ ms of propagation waiting).
        JSONArray cached = api.retrieveCachedRepos();
        populateRepos(cached);

        // For Thread Safe Propagating Variables Across Threads
        AtomicReference<JSONArray> repos = new AtomicReference<>();

        Runnable updateUI = () -> {
            populateRepos(repos.get(), tryAgain);
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
        populateRepos(repos, false);
    }

    private void populateRepos(JSONArray repos, boolean tryAgain) {
        if(repos == null)
            return;

        // TODO: Determine How User Wants Repos Sorted
        // This sorting only works when clearing all previous views from screen and storing all of them in cache
//        repos = HelperMethods.sortReposBySize(repos);

        // For Populating
        LinearLayout repoView = findViewById(R.id.repos);
//        repoView.removeAllViews(); // Clear Existing Views

        // Remove Placeholder View Once We Have Data To Populate With
        if(repoView.findViewById(R.id.placeholder_repo) != null)
            repoView.removeView(repoView.findViewById(R.id.placeholder_repo));

        // The Private GraphQL API Often Repeats Repos We've Already Seen, So Sometimes We Have To Request More Data
        boolean hasAddedMoreData = false;

        // See https://stackoverflow.com/a/46261385/6828099 as to why a For Each Loop Doesn't Work
        int totalRepos = repos.length();
        for(int i = 0; i < totalRepos; i++) {
            boolean foundExistingEntry = false;

            try {
                JSONObject repo = (JSONObject) repos.get(i);
                String id = repo.getString("_id");

                // This prevents showing the same repos repeatedly
                for(int c = 0; c < repoView.getChildCount(); c++) {
                    String tempID = (String) repoView.getChildAt(c).getTag(R.id.repo_id_tag);
                    if(tempID != null && tempID.equals(id)) {
//                        Log.e(tagName, "PREBREAK Found Existing Entry!!! Entry: " + tempID);
                        foundExistingEntry = true;
                        break;
                    }
                }

                if(foundExistingEntry) {
//                    Log.e(tagName, "Found Existing Entry!!! ID: " + i + " Entry: " + id);
                    continue;
                }

                hasAddedMoreData = true;

                String repoName = repo.getString("repoName");
                String ownerName = repo.getString("ownerName");

                String description = repo.getString("description");
                int forks = (int) repo.get("forkCount");
                int stars = (int) repo.get("starCount");

                description = !HelperMethods.strip(description).equals("") ? HelperMethods.strip(description) : getString(R.string.no_description);

                String repoSize = "N/A";
                String rawRepoSize = repo.getString("size");
                try {
                    repoSize = HelperMethods.humanReadableByteCountSI(Long.parseLong(rawRepoSize));
                } catch (NumberFormatException e) {
                    Log.e(tagName, String.format("Repo: %s/%s contains an invalid size `%s`!!!", ownerName, repoName, rawRepoSize));
                }

                String display = String.format("%s/%s - %s - Size %s", ownerName, repoName, description, repoSize);

                Button repoItem = new Button(getApplicationContext());
                repoItem.setTag(R.id.repo_id_tag, id);
                repoItem.setText(display);
//                repoItem.setTextColor(Color.RED);

                repoView.addView(repoItem);

//                Log.d(tagName, display);
            } catch (JSONException e) {
                Log.e(tagName, "Failed To Read Repo Numbered: " + i);
            }
        }

        // Allow User To Manually Try Again
        if(tryAgain) {
            currentTries = 0;
        }

        if(!hasAddedMoreData && (currentTries < maxTries)) {
            Log.e(tagName, "NOT MORE DATA - Current Tries: " + currentTries);

            currentTries += 1;
            retrieveAndPopulateRepos();
        } else if (!hasAddedMoreData && (currentTries >= maxTries)) {
            Log.w(tagName, "Ran Out Of Tries For Loading New Data!!!");
        } else if (hasAddedMoreData) {
            Log.d(tagName, "Added More Data!!! Current Try: " + currentTries);
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

    // Modified From: https://stackoverflow.com/a/47507856/6828099
    public void onScrollChanged(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        double updatePercent = 0.6;

//        if (scrollY > oldScrollY) {
//            Log.d(tagName, "Scrolled Down");
//        }
//        if (scrollY < oldScrollY) {
//            Log.d(tagName, "Scrolled Up");
//        }
//
//        if (scrollY == 0) {
//            Log.d(tagName, "Scrolled To Top");
//        }

        double maximumHeight = v.getChildAt(0).getMeasuredHeight(); // Total Height
        double measuredHeight = v.getMeasuredHeight(); // The height within the constraints of the parent view

//        Log.e(tagName, "L: " + maximumHeight);
//        Log.e(tagName, "M: " + measuredHeight);
        if ((scrollY >= (maximumHeight - measuredHeight)*updatePercent) && scrollY > oldScrollY) {
//            Log.d(tagName, "Scrolled To 80+% Down");
            retrieveAndPopulateRepos(true);
        }

//        if (scrollY == (maximumHeight - measuredHeight)) {
//            Log.d(tagName, "Scrolled To Bottom");
//            retrieveAndPopulateRepos(true);
//        }
    }
}