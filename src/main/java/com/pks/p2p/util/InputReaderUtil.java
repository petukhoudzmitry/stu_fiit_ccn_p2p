package com.pks.p2p.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class InputReaderUtil {

    public static String readInput(InputStream inputStream, Condition condition) {
        StringBuilder result = null;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            while(condition.compute() && result == null) {
                while(bufferedReader.ready()) {
                    if(result == null) result = new StringBuilder();
                    result.append("\n").append(bufferedReader.readLine());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result == null ? null : result.toString().trim();
    }
}
