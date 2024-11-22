package com.pks.p2p.connection;

import com.pks.p2p.sockets.ClientSocket;
import org.jetbrains.annotations.NotNull;

import java.net.*;
import java.util.Objects;

public class Connection {

    private final ClientSocket socket;
    private volatile InetAddress address;
    private volatile int port;
    private volatile boolean connected = false;


    public Connection(ClientSocket socket) {
        this.socket = socket;
    }


    public synchronized void connect(@NotNull InetAddress address, int port) {
        setAddress(address);
        setPort(port);
    }

    public synchronized ClientSocket getSocket() {
        return socket;
    }

    public synchronized InetAddress getAddress() {
        return address;
    }

    public synchronized void setAddress(InetAddress address) {
        this.address = address;
    }

    public synchronized int getPort() {
        return port;
    }

    public synchronized void setPort(int port) {
        this.port = port;
    }

    public synchronized boolean getConnected() {
        return connected;
    }

    public synchronized void setConnected(boolean connected) {
        this.connected = connected;
    }


    public synchronized void stop() {
        setConnected(false);
        Objects.requireNonNull(socket).close();
    }
}
