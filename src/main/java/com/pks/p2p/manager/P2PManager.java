package com.pks.p2p.manager;

import com.pks.p2p.communication.Receiver;
import com.pks.p2p.communication.Sender;
import com.pks.p2p.configs.Configurations;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageType;
import com.pks.p2p.handlers.*;
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
        FinHandler finHandler = new FinHandler(connection, sender);
        MsgHandler msgHandler = new MsgHandler(connection);
        FileHandler fileHandler = new FileHandler(connection);
        this.receiver = new Receiver(connection, List.of(handshakeHandler, keepAliveHandler, finHandler, msgHandler, fileHandler));
    }

    public void send(String data) {
        if (data != null && !data.isEmpty()) {
            if (data.startsWith("file:")) {
                sender.send(MessageType.FILE, data);
            } else {
                sender.send(MessageType.MSG, data);
            }
        }
    }

    public synchronized int getPort() {
        return connection.getSocket().getPort();
    }

    public void start() {
        receiver.listen();
    }

    public synchronized void connect(InetAddress address, int port) {
        connection.connect(address, port);
        handshakeHandler.performHandshake();
    }

    public synchronized void disconnect() {
        while (sender.isSending());
        sender.send(MessageType.FIN, "");
    }

    public synchronized boolean isConnected() {
        return connection.getConnected();
    }

    public synchronized void stop() {
        connection.stop();
        receiver.stop();
        keepAliveHandler.stop();
    }
}
