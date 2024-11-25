package com.pks.p2p.handlers;

import com.pks.p2p.communication.Sender;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageType;
import com.pks.p2p.protocol.Header;
import org.jetbrains.annotations.NotNull;

import java.net.DatagramPacket;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HandshakeHandler implements PackageHandler {

    private final Connection connection;
    private final Sender sender;
    private final KeepAliveHandler keepAliveHandler;
    private final long timeout;

    public HandshakeHandler(Connection connection, Sender sender, KeepAliveHandler keepAliveHandler, long timeout) {
        this.connection = connection;
        this.sender = sender;
        this.keepAliveHandler = keepAliveHandler;
        this.timeout = timeout;
    }

    @Override
    public void receivePackage(@NotNull Header header, @NotNull DatagramPacket packet) {
        if (connection.getConnected()) {
            return;
        }

        MessageType messageType = MessageType.fromInt(header.getMessageType());

        switch(Objects.requireNonNull(messageType)) {
            case MessageType.SYN -> {
                if(!connection.getConnected()) {
                    connection.setAddress(packet.getAddress());
                    connection.setPort(packet.getPort());
                    sender.send(MessageType.SYN_ACK, "", false);
                }
            }
            case MessageType.SYN_ACK -> {
                System.out.println("\nYour are connected to " + connection.getAddress() + ":" + connection.getPort());
                connection.setConnected(true);
                keepAliveHandler.start();
                sender.send(MessageType.ACK, "", false);
                sender.startSending();
            }
            case MessageType.ACK -> {
                System.out.println("\nYou are connected to " + connection.getAddress() + ":" + connection.getPort());
                connection.setConnected(true);
                keepAliveHandler.start();
                sender.startSending();
            }
            default -> {}
        }
    }

    public void performHandshake() {
        long startTime = System.currentTimeMillis();
        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.scheduleAtFixedRate(() -> {
                if (!connection.getConnected()) {
                    sender.send(MessageType.SYN, "", false);
                }
            }, 0, timeout / 10, TimeUnit.MILLISECONDS);
            while (!connection.getConnected() && System.currentTimeMillis() - startTime < timeout) {}
            executor.shutdown();
        }
    }
}
