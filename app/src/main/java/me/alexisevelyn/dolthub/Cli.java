package me.alexisevelyn.dolthub;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class Cli {
    // Expected Version - 0.24.3
    // Version Command `dolt version`

    public static String tagName = "DoltHubDebug";

    public String executeDolt(Context context, String... arguments) {
        return executeDolt(context, null, arguments);
    }

    // The String... is VarArgs
    public String executeDolt(Context context, File cwd, String... arguments) {
        String dolt = context.getApplicationInfo().nativeLibraryDir + "/libdolt.so";
        String homeDir = context.getApplicationInfo().dataDir + "/files";

        String output = null;
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
                Log.e(tagName, home.getAbsolutePath());
            }

            home.mkdirs();
            ps.directory(home);

            // Disable to hide errors from Dolt CLI
            // ps.redirectErrorStream(true);

            Process process = ps.start();

            InputStream inputStream = process.getInputStream();

            while(process.isAlive()) {
                if(output == null) {
                    output = readInputStream(inputStream).toString();
                } else {
                    // I'll rewrite this later, I know concatenating Strings is terrible for performance!!!
                    output += readInputStream(inputStream).toString();
                }
            }

            process.waitFor();
        } catch (IOException|InterruptedException e) {
            // TODO: Make Error Message More Useful
            Log.e(tagName, "Exception While Executing Dolt Binary!!!");
            Log.e(tagName, "Exception: " + e.getLocalizedMessage());
        }

        return output;
    }

    public String readRows(Context context) {
        String query = "select * from detect_environment;";

        String repo_folder = "experiments";
        String results = executeDolt(context, new File(repo_folder), "sql", "-q", query, "-r", "json");

        // JSONParser jsonParser = new JSONParser();
        Log.d(tagName, "Read Rows Output: " + results);
        return results;
    }

    public void cloneRepo(Context context) {
        String repo = "archived_projects/experiments";
        String output = executeDolt(context, "clone", repo);
        Log.d(tagName, "Clone Output: " + output);
    }

    public String getVersion(Context context) {
        return executeDolt(context, "version");
    }

    public StringBuilder readInputStream(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = in.readLine()) != null) {
            builder.append(line);
        }

//        in.close();
        return builder;
    }
}