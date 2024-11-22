package com.pks.p2p.communication;

import com.pks.p2p.configs.Configurations;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageType;
import com.pks.p2p.protocol.DataHeader;
import com.pks.p2p.protocol.Header;
import com.pks.p2p.util.ByteArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Sender {

    private final Connection connection;

    private final AtomicInteger atomicInteger = new AtomicInteger(0);
    private final AtomicLong atomicLong = new AtomicLong(0L);

    private final ConcurrentLinkedQueue<byte[]> messages = new ConcurrentLinkedQueue<>();

    public Sender(Connection connection) {
        this.connection = connection;
    }


    public void startSending() {
        new Thread(() -> {
            while (connection.getConnected()) {
                if (!messages.isEmpty()) {
                    byte[] message = messages.poll();
                    send(message);
                }
            }
        }).start();
    }

    private void send(@NotNull byte[] data) {
        if (data != null) {
            connection.getSocket().send(new DatagramPacket(data, data.length, connection.getAddress(), connection.getPort()));
        }
    }


    public void send(@NotNull MessageType messageType, @NotNull String data) {
        if (data == null) {
            return;
        }

        if (messageType == MessageType.MSG) {
            sendMessage(messageType, data);
            return;
        } else if (messageType == MessageType.FIN_ACK) {
            messages.add(new Header(messageType.getValue(), atomicInteger.getAndIncrement(), 0, new byte[0]).toBytes());
            return;
        }

        new Thread(() -> {
            byte[] bytes = data.getBytes();
            byte[] headerBytes = new Header(messageType.getValue(), atomicInteger.getAndIncrement(), bytes.length, bytes).toBytes();
            ByteBuffer bb = ByteBuffer.allocate(headerBytes.length + bytes.length);
            bb.put(headerBytes);
            bb.put(bytes);

            byte[] message = bb.array();

            switch (messageType) {
                case SYN, SYN_ACK, ACK, KEEP_ALIVE -> send(message);
                default -> messages.add(message);
            }
        }).start();
    }

    private void sendMessage(@NotNull MessageType messageType, @NotNull String data) {
        new Thread(() -> {
            if (data == null || data.isEmpty()) {
                return;
            }

            if (messageType == MessageType.MSG) {

                List<byte[]> chunks = ByteArrayUtil.chunkByteArray(data.getBytes(),
                        Configurations.MAX_PACKET_SIZE - Configurations.HEADER_LENGTH - Configurations.DATA_HEADER_LENGTH
                );

                long messageId = atomicLong.getAndIncrement();

                int headerLength = Configurations.HEADER_LENGTH + Configurations.DATA_HEADER_LENGTH;

                for (int i = 0; i < chunks.size(); i++) {
                    byte[] chunk = chunks.get(i);
                    ByteBuffer bb = ByteBuffer.allocate(headerLength + chunk.length);

                    DataHeader dataHeader = new DataHeader(messageId, chunks.size(), i, new byte[0]);
                    bb.put(Configurations.HEADER_LENGTH, dataHeader.toBytes());
                    bb.put(headerLength, chunk);

                    byte[] dataForChecksum = new byte[headerLength + chunk.length - 8];

                    System.arraycopy(bb.array(), 8, dataForChecksum, 0, dataForChecksum.length);

                    byte[] headerBytes = new Header(
                            messageType.getValue(),
                            atomicInteger.getAndIncrement(),
                            dataHeader.toBytes().length + chunk.length,
                            dataForChecksum
                            ).toBytes();

                    bb.put(headerBytes);

                    byte[] message = bb.array();

                    messages.add(message);
                }
            }
        }).start();
    }

    public synchronized boolean isSending() {
        return !messages.isEmpty();
    }
}
