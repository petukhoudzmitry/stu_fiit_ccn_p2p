package com.pks.p2p.manager;

import com.pks.p2p.communication.Receiver;
import com.pks.p2p.communication.Sender;
import com.pks.p2p.configs.Configurations;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.handlers.HandshakeHandler;
import com.pks.p2p.handlers.KeepAliveHandler;
import com.pks.p2p.handlers.PackageHandler;
import com.pks.p2p.sockets.ClientSocket;

import java.net.InetAddress;
import java.util.List;

public class P2PManager {

    private final Connection connection;
    private final Receiver receiver;
    private final Sender sender;
    private final HandshakeHandler handshakeHandler;
    private final KeepAliveHandler keepAliveHandler;

    public P2PManager(int port) {
        this.connection = new Connection(new ClientSocket(port));
        this.sender = new Sender(connection);
        this.keepAliveHandler = new KeepAliveHandler(connection, sender, Configurations.CONNECTION_TIMEOUT, Configurations.KEEP_ALIVE_INTERVAL);
        this.handshakeHandler = new HandshakeHandler(connection, sender, keepAliveHandler, Configurations.CONNECTION_TIMEOUT);
        this.receiver = new Receiver(connection, List.of(handshakeHandler, keepAliveHandler));
    }


    public int getPort() {
        return connection.getSocket().getPort();
    }

    public void start() {
        receiver.listen();
    }

    public void connect(InetAddress address, int port) {
        connection.connect(address, port);
        handshakeHandler.performHandshake();
    }

    public boolean isConnected() {
        return connection.getConnected();
    }

    public void stop() {
        receiver.stop();
        connection.stop();
        keepAliveHandler.stop();
    }
}
