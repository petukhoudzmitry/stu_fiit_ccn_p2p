package com.pks.p2p.configs;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class Configurations {
    public final static int INPUT_TIMEOUT_SECONDS = 15;
    public static int MAX_PACKET_SIZE = 1000;
    public final static int HEADER_LENGTH = 20;
    public final static int HEADER_LENGTH_WITHOUT_CHECKSUM = 12;
    public final static int DATA_HEADER_LENGTH = 16;
    public final static long CONNECTION_TIMEOUT = 30_000;
    public final static long KEEP_ALIVE_INTERVAL = 5_000;
    public final static int MAX_FRAGMENT_SIZE = 1000;
    public final static int MAX_PORT_NUMBER = 65_535;
    public final static int MIN_FRAGMENT_SIZE = HEADER_LENGTH + DATA_HEADER_LENGTH + 1;
    public final static int ARQ_TIMEOUT = 100;
    public final static int WINDOW_SIZE = 500_000 / MAX_FRAGMENT_SIZE;

    private static String DOWNLOAD_PATH = "downloads";

    public static String getDownloadPath() {
        return DOWNLOAD_PATH;
    }

    public static boolean setDownloadPath(String path) {
        try {
            Paths.get(path);

            File directory = new File(path);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("Directory created: " + directory.getAbsolutePath());
                    DOWNLOAD_PATH = path;
                } else {
                    System.err.println("Failed to create directory: " + directory.getAbsolutePath());
                    return false;
                }
            } else if (!directory.isDirectory()) {
                System.err.println("Path is not a directory: " + directory.getAbsolutePath());
                return false;
            } else {
                DOWNLOAD_PATH = path;
            }
        } catch (InvalidPathException e) {
            return false;
        }

        return true;
    }

    public static boolean setFragmentSize(int size) {
        if (size < MIN_FRAGMENT_SIZE) {
            System.out.println("Fragment size must be greater than " + (MIN_FRAGMENT_SIZE - 1));
        } else if (size > MAX_FRAGMENT_SIZE) {
            System.out.println("Fragment size must be less than " + MAX_FRAGMENT_SIZE);
        } else {
            MAX_PACKET_SIZE = size;
            return true;
        }

        return false;
    }
}
