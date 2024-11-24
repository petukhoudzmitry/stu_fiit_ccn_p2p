package com.pks.p2p.communication;

import com.pks.p2p.configs.Configurations;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageType;
import com.pks.p2p.handlers.PackageHandler;
import com.pks.p2p.protocol.Header;
import com.pks.p2p.util.Checksum;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

public class Receiver {

    private final Connection connection;
    private final Sender sender;
    private final List<PackageHandler> packageHandlers;
    private volatile Thread listenerThread = null;
    private volatile boolean isListening = false;

    public Receiver(Connection connection, Sender sender, List<PackageHandler> packageHandlers) {
        this.connection = connection;
        this.sender = sender;
        this.packageHandlers = packageHandlers;
    }

    public void listen() {
        System.out.println("Listening for incoming packets...");

        setIsListening(true);

        setListenerThread(new Thread(() -> {
            while(getIsListening()) {
//                byte[] buffer = new byte[Configurations.MAX_PACKET_SIZE];
//                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                ByteBuffer buffer = ByteBuffer.allocate(Configurations.MAX_PACKET_SIZE);
                buffer.clear();

                SocketAddress address = connection.getSocket().receive(buffer);


                if (address != null) {

                    InetSocketAddress senderAddress = (InetSocketAddress) address;

//                    if(connection.getConnected() && (!senderAddress.getAddress().equals(connection.getAddress()) || senderAddress.getPort() != connection.getPort())) {
//                        System.out.println("Received a packet from an unknown source. Ignoring...");
//                        continue;
//                    }

                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);

                    Header header = Header.fromBytes(data);

                    ByteBuffer byteBuffer = ByteBuffer.allocate(header.getLength() + Configurations.HEADER_LENGTH_WITHOUT_CHECKSUM);
                    byteBuffer.put(header.toBytesForChecksum()).put(data, Configurations.HEADER_LENGTH, header.getLength());

                    if(Checksum.checkChecksum(byteBuffer.array(), header.getChecksum())) {
                        System.out.println("Received a corrupted packet. Ignoring...");
                        continue;
                    }

                    MessageType messageType = Objects.requireNonNull(MessageType.fromInt(header.getMessageType()));

                    if (messageType == MessageType.MSG || messageType == MessageType.FILE) {
                        Header ackHeader = new Header(MessageType.ACK.getValue(), header.getSequenceNumber());
                        sender.send(ackHeader.toBytes());
                    }

                    DatagramPacket packet = new DatagramPacket(data, data.length, senderAddress.getAddress(), senderAddress.getPort());

                    if (connection.getConnected()) {
                        switch (messageType) {
                            case KEEP_ALIVE, FIN, FIN_ACK, MSG, FILE -> packageHandlers.forEach(handler -> handler.receivePackage(header, packet));
                            case ACK -> sender.confirmPackage(header.getSequenceNumber());
                            default -> {}
                        }
                    } else {
                        switch (messageType) {
                            case SYN, SYN_ACK, ACK -> packageHandlers.forEach(handler -> handler.receivePackage(header, packet));
                            default -> {}
                        }
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
