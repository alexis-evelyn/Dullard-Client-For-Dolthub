package me.alexisevelyn.dullard.utilities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import me.alexisevelyn.dullard.activities.Settings;

public class HelperMethods {
    private static final String tagName = "DullardHelpers";

    public static StringBuilder readInputStream(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = in.readLine()) != null) {
            builder.append(line).append("\n");
        }

//        in.close();
        return builder;
    }

    public static StringBuilder readTextFile(File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }

        reader.close();
        return builder;
    }

    public static void writeTextFile(File file, String string) throws IOException {
        if (!file.exists()) {
            Log.e(tagName, "Creating New File: " + file.getAbsolutePath());
            file.createNewFile();
        }

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

    // Stolen From: https://stackoverflow.com/a/3758880/6828099
    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }

        // TODO: R.string.byte_size_si
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }

    // Stolen From: https://stackoverflow.com/a/3758880/6828099
    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);

        // TODO: R.string.byte_size_binary
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    private static ArrayList<JSONObject> getRepoListForSort(JSONArray repos) {
        ArrayList<JSONObject> list = new ArrayList<>();
        for(int i = 0; i < repos.length(); i++) {
            try {
                list.add(repos.getJSONObject(i));
            } catch (JSONException e) {
                Log.e(tagName, "JSONException While Preparing For Sorting! Exception: " + e.getLocalizedMessage());
            }
        }

        return list;
    }

    private static JSONArray convertListBackToJSONArray(ArrayList<JSONObject> list) {
        JSONArray repos = new JSONArray();
        for(int i = 0; i < list.size(); i++) {
            repos.put(list.get(i));
        }

        return repos;
    }

    public static JSONArray sortReposBySize(JSONArray repos) {
        return sortReposBySize(repos, false);
    }

    public static JSONArray sortReposBySize(JSONArray repos, boolean reversed) {
        ArrayList<JSONObject> list = getRepoListForSort(repos);

        list.sort((Comparator<JSONObject>) (a, b) -> {
            long a_size = 0L;
            long b_size = 0L;

            try {
                a_size = Long.parseLong((String) a.get("size"));
                b_size = Long.parseLong((String) b.get("size"));
            } catch (JSONException e) {
                Log.e(tagName, "JSONException While Sorting! Exception: " + e.getLocalizedMessage());
            }

            return reversed ? Long.compare(a_size, b_size) * -1 : Long.compare(a_size, b_size);
        });

        return convertListBackToJSONArray(list);
    }

    // Open Settings Activity
    public static void openSettings(View view) {
        Intent intent = new Intent(view.getContext(), Settings.class);
        view.getContext().startActivity(intent);
    }

    // This allows me to load the user's chosen Day/Night Mode
    public static void loadDayNightPreferences(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String chosenDayNight = prefs.getString("themes_day_night_preferences", "default-system");

        // The app defaults to System Mode, So We Ignore It If It's System Mode To Speed Up Activity Loading
        if (!chosenDayNight.equals("default-system"))
            setDayNightMode(chosenDayNight);
    }

    // This Checks The User's Preferences For Day/Night Mode And Sets The Mode
    public static boolean setDayNightMode(String day_night_value) {
        switch (day_night_value) {
            case "default-dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                return true;
            case "default-light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                return true;
            case "default-system":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                return true;
            default:
                return false;
        }
    }
}
