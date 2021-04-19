package me.alexisevelyn.dullard.activities;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

import me.alexisevelyn.dullard.R;
import me.alexisevelyn.dullard.utilities.Api;
import me.alexisevelyn.dullard.utilities.Cli;
import me.alexisevelyn.dullard.utilities.HelperMethods;
import me.alexisevelyn.dullard.views.RepoCard;

// TODO: Detect if connected to internet to decide when to retry loading live
public class DiscoverRepos extends AppCompatActivity {
    private static String tagName = "DullardScrolling";

    // For Reusing Same API Object
    private Api api = null;

    // The Private GraphQL API Often Repeats Repos We've Already Seen, So Sometimes We Have To Request More Data
    private int currentTries = 0;
    private int maxTries = 3;
    private int repeatedRanOutOfTriesMessage = 0; // Stop's The App From Being Chatty
    private int maxRepeatedRanOutOfTriesMessage = 1;
    private boolean registeredNetworkListener = false; // To not register the listener more than once

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HelperMethods.loadDayNightPreferences(getApplicationContext());
        setContentView(R.layout.activity_discover_repos);

        // This Activates The Custom Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.round_menu_24); // Set Navigation Icon - Hamburger Menu
        toolbar.setTitle(R.string.discover_repos_title);
        setSupportActionBar(toolbar);

        // Open Hamburger Menu (Sidebar) - TODO: Implement
        toolbar.setNavigationOnClickListener(view -> {
            Snackbar.make(view, R.string.not_implemented, Snackbar.LENGTH_LONG).setAction("Action", null).show();
        });

        // Setup Scrolling Listener
        NestedScrollView nestedScrollView = findViewById(R.id.repos_scroll_view);
        nestedScrollView.setOnScrollChangeListener(this::onScrollChanged);

        toolbar.setOnLongClickListener(view -> {
            Snackbar.make(view, R.string.scrolling_top, Snackbar.LENGTH_LONG).setAction("Action", null).show();

            // This makes instant scroll, no animation
//                nestedScrollView.scrollTo(0, 0);

            // This is fast scroll, animation
            nestedScrollView.fullScroll(View.FOCUS_UP);

            // Neither of these scrolls uncollapse the app bar :(

            return true;
        });

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
        reposThread.setName("Retrieving ReposList Free Attempt (DiscoverRepos)");
        reposThread.start();

        // We get one free update attempt before we register the network change callback
        // After that, we wait until the network is available before trying again
        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
        connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // If Network Becomes Available And We Still Have The Placeholder Card, Update
                LinearLayout repoView = findViewById(R.id.repos);
                if(repoView.findViewById(R.id.placeholder_repo) != null) {
                    Thread reposThread = new Thread(reposRunnable);
                    reposThread.setName("Retrieving ReposList Network Came Online (DiscoverRepos)");
                    reposThread.start();
                }
            }

            @Override
            public void onLost(Network network) {
                // We don't need to do anything when the network is lost right now.
                // Maybe we can add a message or something to alert the user later.
            }
        });

        this.registeredNetworkListener = true;
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
                String repoName = repo.getString("repoName");
                String ownerName = repo.getString("ownerName");
                String id = String.format("%s/%s", ownerName, repoName);

                // This prevents showing the same repos repeatedly
                // TODO: Fix this!!!
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

                String description = repo.getString("description");
                int forks = repo.getInt("forkCount");
                int stars = repo.getInt("starCount");
                long timeStamp = repo.getLong("timestamp");

                description = !HelperMethods.strip(description).equals("") ? HelperMethods.strip(description) : getString(R.string.no_description);

                RepoCard repoItem = new RepoCard(this);
                repoItem.setBackgroundColor(getColor(R.color.transparent)); // Why can't I set this in the RepoCard's XML?
                repoItem.setTag(R.id.repo_id_tag, id);
                repoItem.setOwner(ownerName);
                repoItem.setRepo(repoName);
                repoItem.setDescription(description);

                repoItem.setStars(stars);
                repoItem.setForks(forks);
                repoItem.setTimeStamp(timeStamp);

                try {
                    // Parse Long Out Of String
                    repoItem.setSize(Long.parseLong(repo.getString("size")));
                } catch (NumberFormatException e) {
                    Log.e(tagName, String.format("Repo: %s/%s contains an invalid size `%s`!!!", ownerName, repoName, repo.getString("size")));
                }

                repoItem.setOnClickListener(this::onRepoClickEvent);

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

        // TODO: Get rid of this mess as soon as a public api for listing repos is released
        if(!hasAddedMoreData && (currentTries < maxTries)) {
            repeatedRanOutOfTriesMessage = 0;
            Log.e(tagName, "NOT MORE DATA - Current Tries: " + currentTries);

            currentTries += 1;
            retrieveAndPopulateRepos();
        } else if (!hasAddedMoreData && (currentTries >= maxTries)) {
            if (repeatedRanOutOfTriesMessage < maxRepeatedRanOutOfTriesMessage) {
                repeatedRanOutOfTriesMessage += 1;
                Log.w(tagName, "Ran Out Of Tries For Loading New Data!!!");
            }
        } else if (hasAddedMoreData) {
            repeatedRanOutOfTriesMessage = 0;
            Log.d(tagName, "Added More Data!!! Current Try: " + currentTries);
        }
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
            getVersion(view);

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
        backgroundThread.setName("Retrieving Dolt Version (DiscoverRepos)");
        backgroundThread.start();
    }

    // Modified From: https://stackoverflow.com/a/47507856/6828099
    private void onScrollChanged(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
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

    // Allow Opening More Detailed Page About Repo
    private void onRepoClickEvent(View view) {
        String repoId = (String) view.getTag(R.id.repo_id_tag);
        Log.d(tagName, "Clicked: " + repoId);

        Intent intent = new Intent(this, RepoDetails.class);
        intent.putExtra("id", repoId);
        this.startActivity(intent);
    }
}