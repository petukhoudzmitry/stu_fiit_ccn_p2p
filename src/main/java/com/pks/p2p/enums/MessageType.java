package com.pks.p2p.enums;

public enum MessageType {
    SYN(0),
    SYN_ACK(1),
    ACK(2),
    DATA(3),
    KEEP_ALIVE(4),
    FIN(5),
    FIN_ACK(6),
    MSG(7),
    FILE(8);

    private final int value;

    MessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MessageType fromInt(int value) {
        for (MessageType type : MessageType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }
}
