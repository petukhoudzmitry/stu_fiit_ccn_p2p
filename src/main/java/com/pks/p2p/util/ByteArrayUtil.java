package com.pks.p2p.util;

import java.util.ArrayList;
import java.util.List;

public class ByteArrayUtil {

    public static List<byte[]> chunkByteArray(byte[] array, int chunkSize) {
        List<byte[]> result = new ArrayList<>();
        for (int i = 0; i < array.length; i += chunkSize) {
            byte[] chunk = new byte[Math.min(chunkSize, array.length - i)];
            System.arraycopy(array, i, chunk, 0, chunk.length);
            result.add(chunk);
        }

        return result;
    }
}
