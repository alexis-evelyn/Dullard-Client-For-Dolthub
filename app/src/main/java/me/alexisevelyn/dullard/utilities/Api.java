package me.alexisevelyn.dullard.utilities;

import android.content.Context;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Api {
    private static final String tagName = "DullardApi";

    // TODO: Determine If Token Has Expiration Date!!!
    private String token = null;
    private JSONArray repos = null;

    // Cache Repos
    private final Context context;
    private final File cachedRepos;

    public Api(Context context) {
        this.context = context;
        this.cachedRepos = new File(context.getCacheDir(), "repos.json");

        readRepos();
    }

    private void readRepos() {
        if(cachedRepos.exists()) {
            try {
                String reposString = HelperMethods.readTextFile(cachedRepos).toString();
                this.repos = new JSONArray(reposString);
            } catch (JSONException | IOException e) {
                Log.e(tagName, "Exception Reading Cached Repos!!! Exception: " + e.getLocalizedMessage());
            }
        }
    }

    private void saveRepos() {
        try {
            HelperMethods.writeTextFile(this.cachedRepos, this.repos.toString());
        } catch (FileNotFoundException e) {
            Log.e(tagName, "How does this cached repos file not exist after we check it? Exception: " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(tagName, "Cached Repos IOException: " + e.getLocalizedMessage());
        }
    }

    public JSONArray retrieveCachedRepos() {
        return this.repos;
    }

    public JSONArray listRepos() {
        // Turn on to add all seen repos to cache instead of just the current page
        boolean displayAllRepos = false;

        try {
            JSONObject parameters = getListReposParameters();
            HttpURLConnection connection = getPrivateAPIConnection(parameters);

            int responseCode = connection.getResponseCode();

            if(responseCode == 200) {
                String response = HelperMethods.readInputStream(connection.getInputStream()).toString();
                JSONObject results = new JSONObject(response);

                token = results.getJSONObject("data").getJSONObject("discoverRepos").getString("nextPageToken");

                // I may re-enable/remove functionality to this depending on how the public api will be implemented (we are using the private api)
                //noinspection ConstantConditions
                if(repos == null || !displayAllRepos) {
                    // New List - Never Opened App Before (Or Data Cleared)
                    repos = results.getJSONObject("data").getJSONObject("discoverRepos").getJSONArray("list");
                } else {
                    // Existing List - Append New Entries
                    // TODO: Consider Some Max Size Of Array (Maybe 50 Repos For Example) - Then Delete Oldest Entries Whenever Adding New Entry
                    JSONArray tempArray = results.getJSONObject("data").getJSONObject("discoverRepos").getJSONArray("list");
                    for(int i = 0; i < tempArray.length(); i++) {
                        JSONObject tempRepo = tempArray.getJSONObject(i);

                        boolean notInExistingArray = true;
                        for(int x = 0; x < repos.length(); x++) {
                            // TODO: Check to see if metadata has been updated!!!
                            if(tempRepo.get("_id").equals(repos.getJSONObject(x).get("_id"))) {
                                notInExistingArray = false;
                                break;
                            }
                        }

                        if(notInExistingArray)
                            repos.put(tempRepo);
                    }
                }

//                Log.d(tagName, "Next Page Token: " + token);
                saveRepos();
                return repos;
            } else {
                Log.e(tagName, "GraphQL Status Code (Retrieving Repo List): " + responseCode);
            }
        } catch (IOException | NetworkOnMainThreadException e) {
            Log.e(tagName, "List Repos IOException: " + e);
        } catch (JSONException e) {
            Log.e(tagName, "List Repos JSONException: " + e);
        }

        return null;
    }

    private HttpURLConnection getPrivateAPIConnection(JSONObject parameters) throws IOException {
        URL url = new URL("https://www.dolthub.com/graphql");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", "Dullard-App/0.1");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Content-Type", "application/json");
//            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");

        // Send Post Request Data
        connection.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            wr.writeBytes(parameters.toString());
            wr.flush();
        }

        return connection;
    }

    private HttpURLConnection getPublicAPIConnection(JSONObject parameters, String ownerName, String repoName) throws IOException {
        return getPublicAPIConnection(parameters, ownerName, repoName, null);
    }

    private HttpURLConnection getPublicAPIConnection(JSONObject parameters, String ownerName, String repoName, @SuppressWarnings("SameParameterValue") String branchName) throws IOException {
        URL url;

        if (branchName == null)
            url = new URL(String.format("https://www.dolthub.com/api/v1alpha1/%s/%s", ownerName, repoName));
        else
            url = new URL(String.format("https://www.dolthub.com/api/v1alpha1/%s/%s/%s", ownerName, repoName, branchName));

        try {
            if (parameters != null && parameters.has("q")) {
                String query = parameters.getString("q");
                url = new URL(url, "?q=" + query);
            }
        } catch (JSONException e) {
            Log.e(tagName, "JSONException While Reading Parameters: " + e.getLocalizedMessage());
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Dullard-App/0.1");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Content-Type", "application/json");
//            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");

        return connection;
    }

    public JSONArray getRepoFiles(String repoID) {
        if (repoID == null || !repoID.contains("/"))
            return null;

        // TODO: Implement Class For Owner Name, Repo Name (As Tuples Don't Exist In Java)
        String[] repoIDParts = repoID.split("/");
        String ownerName = repoIDParts[0];
        String repoName = repoIDParts[1];

        return getRepoFiles(ownerName, repoName);
    }

    public JSONArray getRepoFiles(String ownerName, String repoName) {
        try {
            JSONObject parameters = new JSONObject();
            parameters.put("q", "select * from dolt_docs;");

            HttpURLConnection connection = getPublicAPIConnection(parameters, ownerName, repoName);

            int responseCode = connection.getResponseCode();

            if(responseCode == 200) {
                String response = HelperMethods.readInputStream(connection.getInputStream()).toString();
                JSONObject results = new JSONObject(response);

                return results.getJSONArray("rows");
            } else {
                Log.e(tagName, "Public API Status Code (Retrieving Repo Files): " + responseCode);
            }
        } catch (IOException | JSONException e) {
            Log.e(tagName, "Exception Retrieving Repo Files! Exception: " + e.getLocalizedMessage());
        }

        return null;
    }

    public JSONObject getRepoDescription(String repoID) {
        if (repoID == null || !repoID.contains("/"))
            return null;

        // TODO: Implement Class For Owner Name, Repo Name (As Tuples Don't Exist In Java)
        String[] repoIDParts = repoID.split("/");
        String ownerName = repoIDParts[0];
        String repoName = repoIDParts[1];

        return getRepoDescription(ownerName, repoName);
    }

    public JSONObject getRepoDescription(String ownerName, String repoName) {
        try {
            JSONObject parameters = getRepoDescriptionParameters(ownerName, repoName);
            HttpURLConnection connection = getPrivateAPIConnection(parameters);

            int responseCode = connection.getResponseCode();

            if(responseCode == 200) {
                String response = HelperMethods.readInputStream(connection.getInputStream()).toString();
                JSONObject results = new JSONObject(response);

                return results.getJSONObject("data").getJSONObject("repo");
            } else {
                Log.e(tagName, "GraphQL Status Code (Retrieving Repo Description): " + responseCode);
            }
        } catch (IOException | JSONException e) {
            Log.e(tagName, "Exception Getting Repo Description! Exception: " + e.getLocalizedMessage());
        }

        return null;
    }

    private JSONObject getRepoDescriptionParameters(String ownerName, String repoName) throws IOException, JSONException {
        InputStream assetsStream = context.getAssets().open("graphQLRepoDescriptionQuery.txt");
        String query = HelperMethods.readInputStream(assetsStream).toString();

        // Create Post Parameters
        JSONObject parameters = new JSONObject();
        parameters.put("operationName", "RepoForAboutQuery");
        parameters.put("query", query);

        // Add In Variables To Post Parameters If Any
        JSONObject variables = new JSONObject();
        variables.put("ownerName", ownerName);
        variables.put("repoName", repoName);

        parameters.put("variables", variables);

        return parameters;
    }

    private JSONObject getListReposParameters() throws IOException, JSONException {
        InputStream assetsStream = context.getAssets().open("graphQLListReposQuery.txt");
        String query = HelperMethods.readInputStream(assetsStream).toString();

        // Create Post Parameters
        JSONObject parameters = new JSONObject();
        parameters.put("operationName", "RepoListForDiscover");
        parameters.put("query", query);

        // Add In Variables To Post Parameters If Any
        JSONObject variables = new JSONObject();
        if(token != null)
            variables.put("pageToken", token);
        parameters.put("variables", variables);

        return parameters;
    }
}
