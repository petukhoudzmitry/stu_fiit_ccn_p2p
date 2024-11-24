package com.pks.p2p.communication;

import com.pks.p2p.configs.Configurations;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageType;
import com.pks.p2p.handlers.PackageHandler;
import com.pks.p2p.protocol.Header;
import com.pks.p2p.util.Checksum;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

public class Receiver {

    private final Connection connection;
    private final List<PackageHandler> packageHandlers;
    private volatile Thread listenerThread = null;
    private volatile boolean isListening = false;


    public Receiver(Connection connection, List<PackageHandler> packageHandlers) {
        this.connection = connection;
        this.packageHandlers = packageHandlers;
    }

    public void listen() {
        System.out.println("Listening for incoming packets...");

        setIsListening(true);

        setListenerThread(new Thread(() -> {
            while(getIsListening()) {
                byte[] buffer = new byte[Configurations.MAX_PACKET_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                connection.getSocket().receive(packet);

                if(connection.getConnected() && (!packet.getAddress().equals(connection.getAddress()) || packet.getPort() != connection.getPort())) {
                    System.out.println("Received a packet from an unknown source. Ignoring...");
                    continue;
                }

                byte[] data = packet.getData();
                Header header = Header.fromBytes(data);
                ByteBuffer byteBuffer = ByteBuffer.allocate(header.getLength() + Configurations.HEADER_LENGTH_WITHOUT_CHECKSUM);
                byteBuffer.put(header.toBytesForChecksum()).put(data, Configurations.HEADER_LENGTH, header.getLength());

                if(Checksum.checkChecksum(byteBuffer.array(), header.getChecksum())) {
                    System.out.println("Received a corrupted packet. Ignoring...");
                    continue;
                }

                MessageType messageType = Objects.requireNonNull(MessageType.fromInt(header.getMessageType()));

                if (connection.getConnected()) {
                    switch (messageType) {
                        case KEEP_ALIVE, FIN, FIN_ACK, MSG, FILE -> packageHandlers.forEach(handler -> handler.receivePackage(header, packet));
                        default -> {}
                    }
                } else {
                    switch (messageType) {
                        case SYN, SYN_ACK, ACK -> packageHandlers.forEach(handler -> handler.receivePackage(header, packet));
                        default -> {}
                    }
                }
            }
        }));

        getListenerThread().start();
    }

    public void stop() {
        setIsListening(false);
        getListenerThread().interrupt();
    }

    private synchronized void setListenerThread(Thread listenerThread) {
        this.listenerThread = listenerThread;
    }

    private synchronized Thread getListenerThread() {
        return listenerThread;
    }

    private synchronized void setIsListening(boolean isListening) {
        this.isListening = isListening;
    }

    private synchronized boolean getIsListening() {
        return isListening;
    }
}
