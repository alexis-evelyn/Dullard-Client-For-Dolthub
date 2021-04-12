package me.alexisevelyn.dolthub.utilities;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class Cli {
    // Expected Version - 0.24.3
    // Version Command `dolt version`

    private static String tagName = "DoltCli";
    private Context context = null;

    public Cli(Context context) {
        this.context = context;
    }

    public String executeDolt(String... arguments) {
        return executeDolt(null, arguments);
    }

    // The String... is VarArgs
    public String executeDolt(File cwd, String... arguments) {
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

            home.mkdirs();
            ps.directory(home);

            // Disable to hide errors from Dolt CLI
            // ps.redirectErrorStream(true);

            Process process = ps.start();

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

    public String readRows() {
        String query = "select * from detect_environment;";

        String repo_folder = "experiments";
        String results = executeDolt(new File("repos", repo_folder), "sql", "-q", query, "-r", "json");

        // JSONParser jsonParser = new JSONParser();
        Log.d(tagName, "Read Rows Output: " + results);
        return results;
    }

    public void cloneRepo() {
        String repo = "archived_projects/experiments";
        String output = executeDolt(new File("repos"), "clone", repo);
        Log.d(tagName, "Clone Output: " + output);
    }

    public String getVersion() {
        return executeDolt("version");
    }
}