package me.alexisevelyn.dolthub;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class RepoDetails extends AppCompatActivity {
    private static String tagName = "DoltRepoDetails";

    Api api;
    String repoId;
    JSONObject repoDescription;
    SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repo_details);

        // TODO: Add Ability To Parse Repo ID From URL
        this.api = new Api(getApplicationContext());
        boolean foundRepoName = getRepoFromIntent();

        if (!foundRepoName) {
            // Set Title
            if (this.repoId == null || HelperMethods.strip(this.repoId).equals(""))
                setTitle(getString(R.string.invalid_repo_specified_title));
            else {
                setTitle(HelperMethods.strip(this.repoId));
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

        setTitle(this.repoId);
        loadAndPopulateRepoDescription();

        // To allow user to refresh repo details
        this.refreshLayout = findViewById(R.id.refresh_repo_details);
        refreshLayout.setOnRefreshListener(
                () -> loadAndPopulateRepoDescription()
        );
    }

    private boolean getRepoFromIntent() {
        Intent intent = getIntent();

        if (intent.hasExtra("id")) {
            this.repoId = intent.getStringExtra("id");
            return true;
        } else if (intent.getData() != null) {
            String action = intent.getAction();
            Uri data = intent.getData();
            String data_str = data.toString();

//            Log.d(tagName, "Action: " + action); // e.g. android.intent.action.VIEW
//            Log.e(tagName, "Data: " + data); // e.g. dolthub://repo/...

            // TODO: Implement Means Of Retrieving Pull Request, etc... from URI
            if (data_str.contains("://repo/")) {
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
            setTitle(String.format("%s - %s", this.repoId, size));
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