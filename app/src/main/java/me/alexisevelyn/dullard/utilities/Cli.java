package me.alexisevelyn.dullard.utilities;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class Cli {
    private static final String tagName = "DullardCli";
    private final Context context;
    private Process process = null;
    private boolean isSQLServerRunning = false;

    public Cli(Context context) {
        this.context = context;
    }

    private String executeDolt(String... arguments) {
        return executeDolt(null, arguments);
    }

    // The String... is VarArgs
    private String executeDolt(File cwd, String... arguments) {
        String dolt = context.getApplicationInfo().nativeLibraryDir + "/libdolt.so";
        String homeDir = context.getApplicationInfo().dataDir + "/files";

        StringBuilder output = null;
        try {
            String[] command = new String[arguments.length+1];
            command[0] = dolt;

            // I've been spoiled by Python lists :P
            System.arraycopy(arguments, 0, command, 1, arguments.length);

            ProcessBuilder ps = new ProcessBuilder(command);

            // Retrieve Environment Variables And Set Home
            Map<String, String> env = ps.environment();
            env.clear();
            env.put("HOME", homeDir);

            // Set Current Working Directory
            File home;
            if(cwd == null) {
                home = new File(homeDir);
            } else {
                home = new File(homeDir, cwd.getPath());
                Log.d(tagName, "Home Directory: " + home.getAbsolutePath());
            }

            //noinspection ResultOfMethodCallIgnored
            home.mkdirs();
            ps.directory(home);

            // Disable to hide errors from Dolt CLI
//            ps.redirectErrorStream(true);

            this.process = ps.start();

            InputStream inputStream = process.getInputStream();

            while(process.isAlive()) {
                // The reason we use null is a trick I learned from Python in order
                // to easily tell if any output was sent or not.
                if(output == null) {
                    // Replace Null With First String
                    output = HelperMethods.readInputStream(inputStream);
                } else {
                    // Append Onto Existing String
                    output.append(HelperMethods.readInputStream(inputStream));
                }
            }

            process.waitFor();
        } catch (IOException|InterruptedException e) {
            // TODO: Make Error Message More Useful
            Log.e(tagName, "Exception While Executing Dolt Binary!!!");
            Log.e(tagName, "Exception: " + e.getLocalizedMessage());
        }

        // This is a ternary operator
        return (output == null) ? null : output.toString();
    }

    public JSONArray executeQuery(String repoFolder, String query) throws JSONException {
        String results = executeDolt(new File("repos", repoFolder), "sql", "-q", query, "-r", "json");

        JSONArray rows = null;
        if (results != null)
            rows = (JSONArray) new JSONObject(results).get("rows");

        return rows;
    }

    public JSONArray retrieveTables(String repoFolder) throws JSONException {
        return this.executeQuery(repoFolder, "show tables;");
    }

    public boolean isSQLServerRunning() {
        return this.isSQLServerRunning;
    }

    public void stopSQLServer() {
        this.process.destroy();
        this.isSQLServerRunning = false;
    }

    public String startSQLServer(String repoFolderName) {
        File repoFolder = new File("repos", repoFolderName);
        File absoluteRepoPath = new File(context.getApplicationInfo().dataDir + "/files", repoFolder.getPath());
        File configFile = new File(absoluteRepoPath, "config.yaml");

        try {
            // Read Config From Assets
            InputStream configFileInputStream = context.getAssets().open("sql-server-config.yaml");

            // Store Config In Repo Folder
            HelperMethods.writeTextFile(configFile, HelperMethods.readInputStream(configFileInputStream).toString());

            // Mark Server As Running
            this.isSQLServerRunning = true;

            // Execute SQL Server With Stored Config
            String results = this.executeDolt(repoFolder, "sql-server", "--config=config.yaml");

            // Delete Config From Repo Folder
            configFile.delete();

            // Return Results
            return results;
        } catch (IOException e) {
            Log.e(tagName, "Exception Reading Config File!!! Exception: " + e.getLocalizedMessage());
        }

        return "Failed Starting Server!!!";
    }

    public void cloneRepo(String repoID) {
        String output = executeDolt(new File("repos"), "clone", repoID);
        Log.d(tagName, "Clone Output: " + output);
    }

    public String getVersion() {
        return executeDolt("version");
    }
}