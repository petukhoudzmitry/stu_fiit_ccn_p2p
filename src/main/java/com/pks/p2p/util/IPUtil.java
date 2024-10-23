package com.pks.p2p.util;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

public class IPUtil {

    @Nullable
    public static String getIP() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URI("https://checkip.amazonaws.com").toURL().openStream()))){
            return reader.readLine();
        } catch (IOException | URISyntaxException e) {
            return "127.0.0.1";
        }
    }
}
