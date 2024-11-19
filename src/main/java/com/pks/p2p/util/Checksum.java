package com.pks.p2p.util;

import java.util.zip.CRC32;

public class Checksum {
    public static long calculateChecksum(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    public static boolean checkChecksum(byte[] data, long checksum) {
        return Checksum.calculateChecksum(data) != checksum;
    }
}
