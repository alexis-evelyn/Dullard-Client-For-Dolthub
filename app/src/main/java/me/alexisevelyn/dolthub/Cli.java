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

//    static {
//        try {
//            System.loadLibrary("dolt");
//        } catch(Exception e) {
//            Log.e(tagName, "Could not Load Dolt Cli!!!");
//            Log.e(tagName, "Exception: " + e);
//        }
//    }

    public String getVersion(Context context) {
        String dolt = context.getApplicationInfo().nativeLibraryDir + "/libdolt.so";
        String homeDir = context.getApplicationInfo().dataDir + "/files";

        String output = null;
        try {
            File file = context.getFileStreamPath("dolt-arm64");
            Log.d(tagName, "Can Execute: " + file.canExecute());


            ProcessBuilder ps = new ProcessBuilder(dolt, "version");

            // Retrieve Environment Variables And Set Home
            Map<String, String> env = ps.environment();
            env.clear();
            env.put("HOME", homeDir);

            // Set Current Working Directory
            ps.directory(new File(homeDir));

            // Disable to hide errors from Dolt CLI
            // ps.redirectErrorStream(true);

            Process process = ps.start();

            InputStream inputStream = process.getInputStream();
            output = readInputStream(inputStream);

            process.waitFor();
        } catch (IOException|InterruptedException e) {
            // TODO: Make Error Message More Useful
            Log.e(tagName, "Exception While Executing Dolt Binary!!!");
            Log.e(tagName, "Exception: " + e.getLocalizedMessage());
        }

        Log.i(tagName, "Version: `" + output + "`");
        return output;
    }

    public String readInputStream(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = in.readLine()) != null) {
            builder.append(line);
        }

        in.close();
        return builder.toString();
    }
}