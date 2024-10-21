package com.pks.p2p.enums;

public enum MessageTypes {
    SYN(0),
    SYN_ACK(1),
    ACK(2),
    DATA(3),
    KEEP_ALIVE(4);

    private final int value;

    MessageTypes(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
