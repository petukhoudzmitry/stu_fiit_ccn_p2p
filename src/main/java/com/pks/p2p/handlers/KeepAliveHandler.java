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


public class KeepAliveHandler implements PackageHandler {

    private final Connection connection;
    private final Sender sender;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final long timeout;
    private final long interval;
    private long lastReceived = System.currentTimeMillis();

    public KeepAliveHandler(Connection connection, Sender sender, long timeout, long interval) {
        this.connection = connection;
        this.sender = sender;
        this.timeout = timeout;
        this.interval = interval;
    }

    @Override
    public void receivePackage(@NotNull Header header, @NotNull DatagramPacket packet) {
        if (Objects.requireNonNull(MessageType.fromInt(header.getMessageType())) == MessageType.KEEP_ALIVE) {
            onKeepAliveReceived();
        }
    }

    public void start() {
        executorService.scheduleAtFixedRate(this::sendKeepAlive, 0, interval, TimeUnit.MILLISECONDS);
        executorService.scheduleAtFixedRate(this::checkTimeout, 0, interval, TimeUnit.MILLISECONDS);
    }

    private void sendKeepAlive() {
        sender.send(MessageType.KEEP_ALIVE, "");

    }

    private void checkTimeout() {
        if (System.currentTimeMillis() - lastReceived > timeout) {
            System.out.println("Connection timed out.");
            stop();
        }
    }

    private void onKeepAliveReceived() {
        lastReceived = System.currentTimeMillis();
    }

    public void stop() {
        connection.setConnected(false);
        executorService.shutdown();
    }
}
