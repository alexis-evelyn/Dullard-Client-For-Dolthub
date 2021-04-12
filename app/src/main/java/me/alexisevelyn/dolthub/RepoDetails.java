package me.alexisevelyn.dolthub;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

public class RepoDetails extends AppCompatActivity {
    private static String tagName = "DoltRepoDetails";

    Api api;
    String repoId;
    JSONObject repoDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repo_details);

        // TODO: Add Ability To Parse Repo ID From URL
        this.api = new Api(getApplicationContext());
        this.repoId = getIntent().getStringExtra("id");

        setTitle(this.repoId);
        loadAndPopulateRepoDescription();
    }

    private void loadAndPopulateRepoDescription() {
        // TODO: Add Ability To Check From Cache First Then Live Update
        Log.d(tagName, "Loading Repo Details!!! ID: " + repoId);

        // Load Repo Description From API
        getRepoDescriptionFromNetwork();
    }

    private void populateRepoDescription(JSONObject repoDescription) {
        // This is ran on the UI Thread
//        Log.d(tagName, repoDescription.toString());

        TextView descriptionView = findViewById(R.id.repo_description);
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

        Runnable updateUI = () -> populateRepoDescription((JSONObject) backgroundReturnValue.get());

        Runnable backgroundRunnable = () -> {
            backgroundReturnValue.set(api.getRepoDescription(repoId));

            runOnUiThread(updateUI);
        };

        Thread backgroundThread = new Thread(backgroundRunnable);
        backgroundThread.start();
    }
}