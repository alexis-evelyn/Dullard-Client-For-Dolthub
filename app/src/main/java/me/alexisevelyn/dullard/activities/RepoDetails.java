package me.alexisevelyn.dullard.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import me.alexisevelyn.dullard.R;
import me.alexisevelyn.dullard.utilities.Api;
import me.alexisevelyn.dullard.utilities.Cli;
import me.alexisevelyn.dullard.utilities.HelperMethods;

public class RepoDetails extends AppCompatActivity {
    private static String tagName = "DullardRepoDetails";

    private Api api;
    private String repoId;
    private JSONObject repoDescription;
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HelperMethods.loadDayNightPreferences(getApplicationContext());
        setContentView(R.layout.activity_repo_details);

        // This Activates The Custom Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.round_arrow_back_24); // Set Navigation Icon - Back Arrow
        setSupportActionBar(toolbar);

        // This Has The Navigation Button Go Back To The Last Activity
        toolbar.setNavigationOnClickListener(view -> finish());

        // TODO: Add Ability To Parse Repo ID From URL
        this.api = new Api(getApplicationContext());
        boolean foundRepoName = getRepoFromIntent();

        // Handles If Repo Name Is Null From App Or Intent
        if (!foundRepoName || this.repoId == null) {
            // Set Title
            if (this.repoId == null || HelperMethods.strip(this.repoId).equals("")) {
                toolbar.setSubtitle(getString(R.string.invalid_repo_specified_title));
            } else {
                toolbar.setSubtitle(HelperMethods.strip(this.repoId));
            }

            // Set Description
            TextView descriptionView = findViewById(R.id.repo_description);
            descriptionView.setText(getString(R.string.invalid_repo_specified_description));

            // To fake responsiveness of refreshing repo
            this.refreshLayout = findViewById(R.id.refresh_repo_details);
            refreshLayout.setOnRefreshListener(
                    () -> refreshLayout.setRefreshing(false)
            );
            return;
        }

        toolbar.setSubtitle(this.repoId);
        loadAndPopulateRepoDescription();

        // To allow user to refresh repo details
        this.refreshLayout = findViewById(R.id.refresh_repo_details);
        refreshLayout.setOnRefreshListener(this::loadAndPopulateRepoDescription);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            HelperMethods.openSettings(view);

            return true;
        } else if (id == R.id.action_share_repo) {
            String repoLink = String.format("https://www.dolthub.com/repositories/%s", this.repoId);

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");

            // I'm getting rid of the subject for now as the subject isn't
            //   introducing data that can't be inferred from the link itself.
//            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, repoName);
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, repoLink);
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_repo_via)));
            return true;
        } else if (id == R.id.action_clone_repo) {
            cloneRepo(view, this.repoId);

            return true;
        }

        return true;
    }

    private void cloneRepo(View view, String repoName) {
        // TODO: Check if repo already cloned
        Snackbar.make(view, String.format(getString(R.string.cloning_repo), repoName), Snackbar.LENGTH_LONG).setAction("Action", null).show();

        AtomicReference<Object> backgroundReturnValue = new AtomicReference<>();

        Runnable updateUI = () -> {
            TextView tablesTest = findViewById(R.id.repo_tables_test);
            tablesTest.setText((String) backgroundReturnValue.get());
        };

        Runnable backgroundRunnable = () -> {
            Cli cli = new Cli(getApplicationContext());
            cli.cloneRepo(repoName);

            backgroundReturnValue.set(HelperMethods.strip(cli.retrieveTables(this.getRepoFolderName())));

            runOnUiThread(updateUI);
        };

        Thread backgroundThread = new Thread(backgroundRunnable);
        backgroundThread.start();
    }

    private String getRepoFolderName() {
        long slashCount = this.repoId.chars().filter(ch -> ch == '/').count();

        if (slashCount != 1)
            return null;

        // TODO: Implement Repo Details Class To Handle All This Instead Of Repeating Regex
        return this.repoId.split("/")[1];
    }

    private boolean getRepoFromIntent() {
        Intent intent = getIntent();

        if (intent.hasExtra("id")) {
            this.repoId = intent.getStringExtra("id");
            return true;
        } else if (intent.getData() != null) {
//            String action = intent.getAction();
            Uri data = intent.getData();
            String data_str = data.toString();

//            Log.d(tagName, "Action: " + action); // e.g. android.intent.action.VIEW
//            Log.e(tagName, "Data: " + data); // e.g. dolthub://repo/...

            // TODO: Implement Means Of Retrieving Pull Request, etc... from URI
            if (data_str.contains("://repo/")) {
                // Example: dolt://repo/archived_projects/experiments
                String[] validateRepoURI = data_str.split("://repo/");

                if (validateRepoURI.length < 2)
                    return false;

                this.repoId = HelperMethods.strip(validateRepoURI[1], "/");
                long slashCount = this.repoId.chars().filter(ch -> ch == '/').count();

                // Note: The GraphQL api can work with the extra data just fine (so slashCount is not needed),
                //   I'm just imposing arbitrary limits to allow for consistency.

                // Verify An Actual Repo
                if (slashCount != 1)
                    return false;

                Log.d(tagName, "App Repo ID: " + repoId);
                return true;
            } else if (data_str.contains("dolthub.com/repositories/")) {
                // Example: https://www.dolthub.com/repositories/archived_projects/experiments
                String[] validateRepoURI = data_str.split("dolthub\\.com/repositories/");

                if (validateRepoURI.length < 2)
                    return false;

                this.repoId = HelperMethods.strip(validateRepoURI[1], "/");
                long slashCount = this.repoId.chars().filter(ch -> ch == '/').count();

                // Verify An Actual Repo - We can just use contains(...)
                if (slashCount < 1)
                    return false;

                // Only Pick First Two Items From Slash Split
                String[] repoSplit = this.repoId.split("/");
                this.repoId = String.format("%s/%s", repoSplit[0], repoSplit[1]);

                Log.d(tagName, "Web Repo ID: " + repoId);
                return true;
            } else if (data_str.contains("doltremoteapi.dolthub.com/")) {
                // Example: https://doltremoteapi.dolthub.com/archived_projects/experiments
                String[] validateRepoURI = data_str.split("doltremoteapi\\.dolthub\\.com/");

                if (validateRepoURI.length < 2)
                    return false;

                this.repoId = HelperMethods.strip(validateRepoURI[1], "/");

                // This URI cannot contain prs and other data
                if (!this.repoId.contains("/"))
                    return false;

                String[] repoSplit = this.repoId.split("/");
                this.repoId = String.format("%s/%s", repoSplit[0], repoSplit[1]);

                Log.d(tagName, "Remote Repo ID: " + repoId);
                return true;
            }

            return false;
        }

        return false;
    }

    private void loadAndPopulateRepoDescription() {
        // TODO: Determine if should attempt live update first then load from cache if any, then fail
        // Either potentially take a while attempting live update for always fresh info or load fast with cached data

        // TODO: Add Ability To Check From Cache First Then Live Update
        Log.d(tagName, "Loading Repo Details!!! ID: " + repoId);

        // Load Repo Description From API
        getRepoDescriptionFromNetwork();
    }

    private void populateRepoDescription() {
        // This is ran on the UI Thread
//        Log.d(tagName, repoDescription.toString());

        TextView descriptionView = findViewById(R.id.repo_description);
        if (repoDescription == null) {
            descriptionView.setText(getString(R.string.null_repo_description));
            return;
        }

        try {
            String description = repoDescription.getString("description");
            description = (!HelperMethods.strip(description).equals("")) ? description : getString(R.string.no_description);

            String rawSize = repoDescription.getString("size");
            String size = HelperMethods.humanReadableByteCountSI(Long.parseLong(rawSize));

            descriptionView.setText(description);

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setSubtitle(String.format("%s - %s", this.repoId, size));
        } catch (JSONException e) {
            Log.e(tagName, "JSONException While Populating Repo Description! Exception: " + e.getLocalizedMessage());
            descriptionView.setText(getString(R.string.error_loading_repo_description));
        }
    }

    private void getRepoDescriptionFromNetwork() {
        // This executes the network on a background thread
        AtomicReference<Object> backgroundReturnValue = new AtomicReference<>();

        Runnable updateUI = () -> {
            this.repoDescription = (JSONObject) backgroundReturnValue.get();
            populateRepoDescription();
        };

        Runnable backgroundRunnable = () -> {
            // Signals that refreshing is finished
            refreshLayout.setRefreshing(false);

            backgroundReturnValue.set(api.getRepoDescription(repoId));

            runOnUiThread(updateUI);
        };

        Thread backgroundThread = new Thread(backgroundRunnable);
        backgroundThread.start();
    }
}