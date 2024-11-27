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
    private final FileHandler fileHandler;
    private final MsgHandler msgHandler;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final long timeout;
    private final long interval;
    private long lastReceived;

    public KeepAliveHandler(Connection connection, Sender sender, FileHandler fileHandler, MsgHandler msgHandler, long timeout, long interval) {
        this.connection = connection;
        this.sender = sender;
        this.fileHandler = fileHandler;
        this.msgHandler = msgHandler;
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
        lastReceived = System.currentTimeMillis();
        executorService.scheduleAtFixedRate(this::sendKeepAlive, interval, interval, TimeUnit.MILLISECONDS);
        executorService.scheduleAtFixedRate(this::checkTimeout, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void sendKeepAlive() {
        sender.send(MessageType.KEEP_ALIVE, "", false);
    }

    private void checkTimeout() {
        if (System.currentTimeMillis() - lastReceived > timeout) {
            System.out.println("\nConnection timed out.");
            if (fileHandler.hasUnreceivedFiles()) {
                System.out.println("\nThere are unreceived files.");
            }
            if (msgHandler.hasUnreceivedMessages()) {
                System.out.println("\nThere are unreceived messages.");
            }
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
