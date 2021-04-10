package me.alexisevelyn.dolthub;

import android.content.Context;
import android.os.NetworkOnMainThreadException;
import android.os.StrictMode;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Api {
    private static String tagName = "DoltApi";

    // TODO: Determine If Token Has Expiration Date!!!
    private String token = null;

    public JSONArray listRepos(Context context) {
        // TODO: Remove This!!! This allows Networking On Main Thread!!!
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

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
                JSONArray repos = results.getJSONObject("data").getJSONObject("discoverRepos").getJSONArray("list");

                Log.d(tagName, "Next Page Token: " + token);

                return repos;
            } else {
                Log.d(tagName, "GraphQL Status Code (Retrieving Repo List): " + responseCode);
            }
        } catch (IOException | NetworkOnMainThreadException | JSONException e) {
            Log.e(tagName, "IOException: " + e);
        }

        return null;
    }

    private String getQuery(Context context) throws IOException {
        InputStream assetsStream = context.getAssets().open("graphQLQuery.txt");
        return HelperMethods.readInputStream(assetsStream).toString();
    }
}
