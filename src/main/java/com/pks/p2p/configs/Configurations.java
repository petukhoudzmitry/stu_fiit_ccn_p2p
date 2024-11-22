package com.pks.p2p.configs;

public class Configurations {
    public final static int INPUT_TIMEOUT_SECONDS = 15;
    public final static int MAX_PACKET_SIZE = 1400;
    public final static int HEADER_LENGTH = 20;
    public final static int HEADER_LENGTH_WITHOUT_CHECKSUM = 12;
    public final static int DATA_HEADER_LENGTH = 16;
    public final static long CONNECTION_TIMEOUT = 30_000;
    public final static long KEEP_ALIVE_INTERVAL = 5_000;
}
