package me.alexisevelyn.dolthub;

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
}
