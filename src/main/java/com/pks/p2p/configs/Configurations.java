package com.pks.p2p.configs;

public class Configurations {
    public final static int INPUT_TIMEOUT_SECONDS = 15;
    public final static int MAX_PACKET_SIZE = 4096;
    public final static int HEADER_LENGTH = 20;
    public final static int HEADER_LENGTH_WITHOUT_CHECKSUM = 12;
    public final static long CONNECTION_TIMEOUT = 15_000;
    public final static long KEEP_ALIVE_INTERVAL = 5_000;
}
