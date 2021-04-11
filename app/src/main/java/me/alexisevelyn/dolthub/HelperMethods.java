package me.alexisevelyn.dolthub;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HelperMethods {
    public static StringBuilder readInputStream(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = in.readLine()) != null) {
            builder.append(line + "\n");
        }

//        in.close();
        return builder;
    }

    public static StringBuilder readTextFile(File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line + "\n");
        }

        reader.close();
        return builder;
    }

    public static void writeTextFile(File file, String string) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

        bufferedOutputStream.write(string.getBytes());
        bufferedOutputStream.close();
        fileOutputStream.close();
    }

    public static String strip(String original) {
        return strip(original, null);
    }

    public static String strip(String original, String characters) {
        return rstrip(lstrip(original, characters), characters);
    }

    public static String lstrip(String original) {
        return lstrip(original, null);
    }

    public static String lstrip(String original, String characters) {
        if(characters != null) {
            // TODO: Disable Regex On Characters Part
            return original.replaceAll("^" + characters, "");
        }

        return original.replaceAll("^\\s", "");
    }

    public static String rstrip(String original) {
        return rstrip(original, null);
    }

    public static String rstrip(String original, String characters) {
        if(characters != null) {
            // TODO: Disable Regex On Characters Part
            return original.replaceAll(characters + "$", "");
        }

        return original.replaceAll("\\s$", "");
    }
}
