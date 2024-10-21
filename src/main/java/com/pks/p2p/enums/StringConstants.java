package com.pks.p2p.enums;

public enum StringConstants {
    IP_PATTERN("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");

    private final String value;

    StringConstants(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}