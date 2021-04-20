package me.alexisevelyn.dullard.utilities;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicReference;

import io.noties.markwon.Markwon;
import me.alexisevelyn.dullard.R;

// This has no purpose other than to make the Activity class nicer to read
public class RepoDetailsHelper {
    private static final String tagName = "DullardRepoDetailsHelper";

    private final Api api;
    private String repoId;
    private JSONObject repoDescription;
    private JSONArray repoFiles;
    private final AtomicReference<Cli> sqlServerCli = new AtomicReference<>();
    private Markwon markwon;
    private final SwipeRefreshLayout refreshLayout;

    private final AppCompatActivity context;

    public RepoDetailsHelper(AppCompatActivity context) {
        this.context = context;

        // TODO: Add Ability To Parse Repo ID From URL
        this.api = new Api(context);
        boolean foundRepoName = getRepoFromIntent();

        // This Activates The Custom Toolbar
        Toolbar toolbar = context.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.round_arrow_back_24); // Set Navigation Icon - Back Arrow
        context.setSupportActionBar(toolbar);

        // This Has The Navigation Button Go Back To The Last Activity
        toolbar.setNavigationOnClickListener(view -> context.finish());

        // Handles If Repo Name Is Null From App Or Intent
        if (!foundRepoName || this.repoId == null) {
            // Set Title
            if (this.repoId == null || HelperMethods.strip(this.repoId).equals("")) {
                toolbar.setSubtitle(context.getString(R.string.invalid_repo_specified_title));
            } else {
                toolbar.setSubtitle(HelperMethods.strip(this.repoId));
            }

            // Set Description
            TextView descriptionView = context.findViewById(R.id.repo_description);
            descriptionView.setText(context.getString(R.string.invalid_repo_specified_description));

            // To fake responsiveness of refreshing repo
            this.refreshLayout = context.findViewById(R.id.refresh_repo_details);
            refreshLayout.setOnRefreshListener(
                    () -> refreshLayout.setRefreshing(false)
            );
            return;
        }

        toolbar.setSubtitle(this.repoId);

        this.markwon = Markwon.create(context);
        loadAndPopulateRepoData();

        // To allow user to refresh repo details
        this.refreshLayout = context.findViewById(R.id.refresh_repo_details);
        refreshLayout.setOnRefreshListener(this::loadAndPopulateRepoData);
    }

    public void shareRepo() {
        String repoLink = String.format("https://www.dolthub.com/repositories/%s", this.repoId);

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");

        // I'm getting rid of the subject for now as the subject isn't
        //   introducing data that can't be inferred from the link itself.
//            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, repoName);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, repoLink);
        this.context.startActivity(Intent.createChooser(sharingIntent, this.context.getString(R.string.share_repo_via)));
    }

    public void stopSQLServer(View view) {
        if (this.sqlServerCli.get() == null || !this.sqlServerCli.get().isSQLServerRunning()) {
            Snackbar.make(view, String.format(context.getString(R.string.not_running_sql_server), this.repoId), Snackbar.LENGTH_LONG).setAction("Action", null).show();
            return;
        }

        this.sqlServerCli.get().stopSQLServer();
        Snackbar.make(view, String.format(context.getString(R.string.stopping_sql_server), this.repoId), Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    public void startSQLServer(View view) {
        // TODO: Check if repo is cloned
        if (this.sqlServerCli.get() != null && this.sqlServerCli.get().isSQLServerRunning()) {
            Snackbar.make(view, String.format(context.getString(R.string.already_started_sql_server), this.repoId), Snackbar.LENGTH_LONG).setAction("Action", null).show();
            return;
        }

        Snackbar.make(view, String.format(context.getString(R.string.starting_sql_server), this.repoId), Snackbar.LENGTH_LONG).setAction("Action", null).show();

        AtomicReference<Object> backgroundReturnValue = new AtomicReference<>();

        Runnable updateUI = () -> {
            TextView tablesTest = this.context.findViewById(R.id.repo_tables_test);
            tablesTest.setText((String) backgroundReturnValue.get());
        };

        // TODO: Fix so this doesn't get halted when the activity is exited by the user.
        //    That way we don't have corrupt clones from the CLI just being halted
        //    (and this is supposed to be a background action anyway).
        Runnable backgroundRunnable = () -> {
            Cli cli = new Cli(context);
            sqlServerCli.set(cli);

            String results = cli.startSQLServer(this.getRepoFolderName());

            // To Prevent Crashing From Missing Results As Results Aren't Important Here
            if (results == null)
                return;

            backgroundReturnValue.set(HelperMethods.strip(results));

            this.context.runOnUiThread(updateUI);
        };

        Thread sqlServer = new Thread(backgroundRunnable);
        sqlServer.setName("SQL Server (context): " + this.repoId);
        sqlServer.start();
    }

    public void cloneRepo(View view) {
        // TODO: Check if repo already cloned
        Snackbar.make(view, String.format(context.getString(R.string.cloning_repo), this.repoId), Snackbar.LENGTH_LONG).setAction("Action", null).show();

        AtomicReference<Object> backgroundReturnValue = new AtomicReference<>();

        Runnable updateUI = () -> {
            TextView tablesTest = this.context.findViewById(R.id.repo_tables_test);
            tablesTest.setText((String) backgroundReturnValue.get());
        };

        // TODO: Fix so this doesn't get halted when the activity is exited by the user.
        //    That way we don't have corrupt clones from the CLI just being halted
        //    (and this is supposed to be a background action anyway).
        Runnable backgroundRunnable = () -> {
            Cli cli = new Cli(context);
            cli.cloneRepo(this.repoId);

            try {
                StringBuilder tablesBuilder = new StringBuilder();
                JSONArray tables = cli.retrieveTables(this.getRepoFolderName());
                int tableLength = tables.length();

                for (int x = 0; x < tableLength; x++) {
                    JSONObject table = tables.getJSONObject(x);
                    String tableName = HelperMethods.strip(table.getString("Table"));

                    tablesBuilder.append(tableName);

                    // This prevents putting a newline on the last line
                    if (x+1 < tableLength)
                        tablesBuilder.append("\n");
                }

                backgroundReturnValue.set(tablesBuilder.toString());
            } catch (JSONException e) {
                Log.e(tagName, "JSONException: Failed To Parse Tables From CLI: " + e.getLocalizedMessage());
            }

            this.context.runOnUiThread(updateUI);
        };

        Thread backgroundThread = new Thread(backgroundRunnable);
        backgroundThread.setName("Cloning (context): " + this.repoId);
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
        Intent intent = this.context.getIntent();

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

    private void loadAndPopulateRepoData() {
        // TODO: Determine if should attempt live update first then load from cache if any, then fail
        // Either potentially take a while attempting live update for always fresh info or load fast with cached data

        // TODO: Add Ability To Check From Cache First Then Live Update
        Log.d(tagName, "Loading Repo Details!!! ID: " + repoId);

        // Load Repo Description From API
        getRepoDescriptionFromNetwork();

        Log.d(tagName, "Loading Repo Files!!! ID: " + repoId);

        // Load Repo Files From API
        getRepoFilesFromNetwork();
    }

    private void populateRepoDescription() {
        // This is ran on the UI Thread
//        Log.d(tagName, repoDescription.toString());

        TextView descriptionView = this.context.findViewById(R.id.repo_description);
        if (this.repoDescription == null) {
            descriptionView.setText(context.getString(R.string.null_repo_description));
            return;
        }

        try {
            String description = this.repoDescription.getString("description");
            description = (!HelperMethods.strip(description).equals("")) ? description : context.getString(R.string.no_description);

            String rawSize = this.repoDescription.getString("size");
            String size = HelperMethods.humanReadableByteCountSI(Long.parseLong(rawSize));

            descriptionView.setText(description);

            Toolbar toolbar = this.context.findViewById(R.id.toolbar);
            toolbar.setSubtitle(String.format("%s - %s", this.repoId, size));
        } catch (JSONException e) {
            Log.e(tagName, "JSONException While Populating Repo Description! Exception: " + e.getLocalizedMessage());
            descriptionView.setText(context.getString(R.string.error_loading_repo_description));
        }
    }

    private void populateRepoFiles() {
        // This is ran on the UI Thread
//        Log.d(tagName, repoDescription.toString());

        TextView readMeView = this.context.findViewById(R.id.read_me);
        TextView licenseView = this.context.findViewById(R.id.license);
        if (this.repoFiles == null) {
            readMeView.setText(context.getString(R.string.no_readme_found));
            licenseView.setText(context.getString(R.string.no_license_found));
            return;
        }

        try {
            boolean foundReadMe = false;
            boolean foundLicense = false;

            int total_files = this.repoFiles.length();
            for (int x = 0; x < total_files; x++) {
                JSONObject file = (JSONObject) this.repoFiles.get(x);
                String fileName = file.getString("doc_name");
                String fileContents = file.getString("doc_text");

                if (fileName.equals("README.md")) {
                    markwon.setMarkdown(readMeView, fileContents);
                    foundReadMe = true;
                } else if (fileName.equals("LICENSE.md")) {
                    markwon.setMarkdown(licenseView, fileContents);
                    foundLicense = true;
                }
            }

            if (!foundReadMe)
                readMeView.setText(context.getString(R.string.no_readme_found));

            if (!foundLicense)
                licenseView.setText(context.getString(R.string.no_license_found));
        } catch (JSONException e) {
            Log.e(tagName, "JSONException While Populating Repo Description! Exception: " + e.getLocalizedMessage());
            readMeView.setText(context.getString(R.string.no_readme_found));
            licenseView.setText(context.getString(R.string.no_license_found));
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
            backgroundReturnValue.set(api.getRepoDescription(repoId));

            // Signals that refreshing is finished
            if (refreshLayout != null) {
                refreshLayout.setRefreshing(false);
                this.context.runOnUiThread(updateUI);
            }
        };

        Thread backgroundThread = new Thread(backgroundRunnable);
        backgroundThread.setName("Retrieving Description (context) From: " + this.repoId);
        backgroundThread.start();
    }

    private void getRepoFilesFromNetwork() {
        // This executes the network on a background thread
        AtomicReference<Object> backgroundReturnValue = new AtomicReference<>();

        Runnable updateUI = () -> {
            this.repoFiles = (JSONArray) backgroundReturnValue.get();
            populateRepoFiles();
        };

        Runnable backgroundRunnable = () -> {
            backgroundReturnValue.set(api.getRepoFiles(repoId));

            // Signals that refreshing is finished
            if (refreshLayout != null) {
                refreshLayout.setRefreshing(false);
                this.context.runOnUiThread(updateUI);
            }
        };

        Thread backgroundThread = new Thread(backgroundRunnable);
        backgroundThread.setName("Retrieving Files (context) From: " + this.repoId);
        backgroundThread.start();
    }
}
