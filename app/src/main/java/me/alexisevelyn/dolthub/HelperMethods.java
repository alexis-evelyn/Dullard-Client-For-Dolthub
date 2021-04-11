package me.alexisevelyn.dolthub;

import android.util.Log;

import java.io.BufferedReader;
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
