package com.pks.p2p.util;

import java.io.InputStream;
import java.util.Objects;

import static com.pks.p2p.util.InputReaderUtil.readInput;

public class UserInputUtil {

    /***
     *
     * @param inputStream - the input stream to read data from
     * @param pattern - the data in the input stream must match this pattern, otherwise it will print the 'errorMessage' and try to read the data again
     * @param errorMessage - this message is printed if the data from inputStream doesn't match pattern
     * @param condition - the function will try to read the data while this condition return true
     * @return the data scanned in the specified pattern or null otherwise
     */
    public static String getUserValue(InputStream inputStream, String pattern, String errorMessage, Condition condition) {
        String ip = readInput(inputStream, condition);

        try {
            while (!Objects.requireNonNull(ip).matches(pattern) && condition.compute()) {
                System.out.print(errorMessage);
                ip = readInput(inputStream, condition);
            }
        } catch (NullPointerException e) {
            return null;
        }

        return ip.matches(pattern) ? ip : null;
    }
}
