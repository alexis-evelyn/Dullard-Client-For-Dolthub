package me.alexisevelyn.dolthub;

import android.content.Context;
import android.os.NetworkOnMainThreadException;
import android.os.StrictMode;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Api {
    private static String tagName = "DoltApi";

    // TODO: Determine If Token Has Expiration Date!!!
    private String token = null;
    private JSONArray repos = null;

    // Cache Repos
    private Context context;
    private File cachedRepos;

    public Api(Context context) {
        this.context = context;
        this.cachedRepos = new File(context.getCacheDir(), "repos.json");

        readRepos();
    }

    private void readRepos() {
        if(cachedRepos.exists()) {
            try {
                String reposString = HelperMethods.readTextFile(cachedRepos).toString();
                repos = new JSONArray(reposString);
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
        // TODO: Remove This!!! This allows Networking On Main Thread!!!
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);

        try {
            String query = getQuery(context);

            // Create Post Parameters
            JSONObject parameters = new JSONObject();
            parameters.put("operationName", "RepoListForDiscover");
            parameters.put("query", query);

            // Add In Variables To Post Parameters If Any
            JSONObject variables = new JSONObject();
            if(token != null)
                variables.put("pageToken", token);
            parameters.put("variables", variables);

            URL url = new URL("https://www.dolthub.com/graphql");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Dolthub-App/0.1");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Content-Type", "application/json");
//            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");

            // Send Post Request Data
            connection.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(parameters.toString());
                wr.flush();
            }

            int responseCode = connection.getResponseCode();

            if(responseCode == 200) {
                String response = HelperMethods.readInputStream(connection.getInputStream()).toString();
                JSONObject results = new JSONObject(response);

                token = results.getJSONObject("data").getJSONObject("discoverRepos").getString("nextPageToken");

                if(repos == null) {
                    // New List - Never Opened App Before (Or Data Cleared)
                    repos = results.getJSONObject("data").getJSONObject("discoverRepos").getJSONArray("list");
                } else {
                    // Existing List - Append New Entries
                    // TODO: Consider Some Max Size Of Array (Maybe 50 Repos For Example) - Then Delete Oldest Entries Whenever Adding New Entry
                    JSONArray tempArray = results.getJSONObject("data").getJSONObject("discoverRepos").getJSONArray("list");
                    for(int i = 0; i < tempArray.length(); i++) {
                        JSONObject tempRepo = tempArray.getJSONObject(i);

                        Boolean notInExistingArray = true;
                        for(int x = 0; x < repos.length(); x++) {
                            // TODO: Change to use Repo ID!!!
                            if(tempRepo.equals(repos.getJSONObject(x))) {
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
                Log.d(tagName, "GraphQL Status Code (Retrieving Repo List): " + responseCode);
            }
        } catch (IOException | NetworkOnMainThreadException e) {
            Log.e(tagName, "List Repos IOException: " + e);
        } catch (JSONException e) {
            Log.e(tagName, "List Repos JSONException: " + e);
        }

        return null;
    }

    private String getQuery(Context context) throws IOException {
        InputStream assetsStream = context.getAssets().open("graphQLQuery.txt");
        return HelperMethods.readInputStream(assetsStream).toString();
    }
}
