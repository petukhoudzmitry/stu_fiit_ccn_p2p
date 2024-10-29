package com.pks.p2p.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

public class InputReaderUtilTest {

    @Test
    public void noInput_readInput_returnsNull() {
        String input = InputReaderUtil.readInput(System.in, () -> true);
        assert null == input;
    }

    @Test
    public void inputWithinTimeout_readInput_returnsInput() {
        String txt = "test";
        String input = InputReaderUtil.readInput(new ByteArrayInputStream(txt.getBytes()), () -> true);
        assert txt.equals(input);
    }

}
