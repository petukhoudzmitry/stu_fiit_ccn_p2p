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
                ByteBuffer buffer = ByteBuffer.allocate(Configurations.MAX_FRAGMENT_SIZE + Configurations.HEADER_LENGTH + Configurations.DATA_HEADER_LENGTH);
                buffer.clear();

                SocketAddress address = connection.getSocket().receive(buffer);

                if (address != null) {
                    InetSocketAddress senderAddress = (InetSocketAddress) address;

                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);

                    Header header = Header.fromBytes(data);

                    byte[] headerBytes = header.toBytesForChecksum();
                    byte[] checksumBytes = new byte[data.length  - Configurations.HEADER_LENGTH + Configurations.HEADER_LENGTH_WITHOUT_CHECKSUM];

                    byte[] checksum = new byte[data.length - checksumBytes.length];

                    System.arraycopy(data, Configurations.HEADER_LENGTH_WITHOUT_CHECKSUM, checksum, 0, checksum.length);

                    ByteBuffer checksumBuffer = ByteBuffer.wrap(checksum);
                    long checksumValue = checksumBuffer.getLong();

                    System.arraycopy(headerBytes, 0, checksumBytes, 0, headerBytes.length);
                    System.arraycopy(data, Configurations.HEADER_LENGTH, checksumBytes, headerBytes.length, data.length - Configurations.HEADER_LENGTH);

                    if(!Checksum.checkChecksum(checksumBytes, checksumValue)) {
                        System.out.println("Received a corrupted fragment with sequence number " + header.getSequenceNumber());
                        Header nackHeader = new Header(MessageType.NACK.getValue(), header.getSequenceNumber());
                        sender.send(nackHeader.toBytes());

                        continue;
                    }

                    MessageType messageType = Objects.requireNonNull(MessageType.fromInt(header.getMessageType()));

                    if (messageType == MessageType.MSG || messageType == MessageType.FILE || messageType == MessageType.KEEP_ALIVE) {
                        Header ackHeader = new Header(MessageType.ACK.getValue(), header.getSequenceNumber());
                        sender.send(ackHeader.toBytes());
                    }

                    DatagramPacket packet = new DatagramPacket(data, data.length, senderAddress.getAddress(), senderAddress.getPort());

                    if (connection.getConnected()) {
                        switch (messageType) {
                            case KEEP_ALIVE, FIN, FIN_ACK, MSG, FILE -> packageHandlers.forEach(handler -> handler.receivePackage(header, packet));
                            case ACK -> sender.confirmPackage(header.getSequenceNumber());
                            case NACK -> sender.resendMessage(header.getSequenceNumber());
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
