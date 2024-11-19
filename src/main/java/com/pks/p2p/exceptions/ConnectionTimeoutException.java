package com.pks.p2p.exceptions;

public class ConnectionTimeoutException extends Exception{
    public ConnectionTimeoutException() {
        super("Connection time out exception");
    }
}
