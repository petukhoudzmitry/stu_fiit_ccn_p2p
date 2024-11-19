package com.pks.p2p.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

public class InputReaderUtilTest {

    @Test
    public void noInput_readInput_returnsNull() {
        long startTime = System.currentTimeMillis();
        String input = InputReaderUtil.readInput(System.in, () -> System.currentTimeMillis() - startTime < 2000);
        assert null == input;
    }

    @Test
    public void inputWithinTimeout_readInput_returnsInput() {
        String txt = "test";
        long startTime = System.currentTimeMillis();
        String input = InputReaderUtil.readInput(new ByteArrayInputStream(txt.getBytes()), () -> System.currentTimeMillis() - startTime < 2000);
        assert txt.equals(input);
    }
}
