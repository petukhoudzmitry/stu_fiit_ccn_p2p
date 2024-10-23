package com.pks.p2p.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.*;

public class InputReaderUtil {


    public static String readInput(int seconds) {

        try(ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            Future<String> inputFuture = executorService.submit(() -> new BufferedReader(new InputStreamReader(System.in)).readLine());
            try {
                return inputFuture.get(seconds, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                inputFuture.cancel(true);
                return "";
            }
        }
    }
}
