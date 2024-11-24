package com.pks.p2p.communication;

import com.pks.p2p.configs.Configurations;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageType;
import com.pks.p2p.protocol.DataHeader;
import com.pks.p2p.protocol.Header;
import com.pks.p2p.util.ByteArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void send(@NotNull MessageType messageType, @NotNull String data) {
        if (data == null) {
            return;
        }

        if (messageType == MessageType.MSG) {
            sendMessage(messageType, data);
            return;
        } else if (messageType == MessageType.FILE){
            sendFile(messageType, data);
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


    private void sendFile(@NotNull MessageType messageType, @NotNull String data) {
        new Thread(() -> {
            if (data == null || data.isEmpty()) {
                return;
            }

            if (messageType == MessageType.FILE) {
                String filePath = data.replace("file:", "");
                File file = new File(filePath);

                try {
                    byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));

                    // Convert the filename to bytes and append a newline
                    byte[] fileNameBytes = (file.getName() + "\n").getBytes(StandardCharsets.UTF_8);

                    // Combine filename and file content
                    byte[] combined = new byte[fileNameBytes.length + fileBytes.length];
                    System.arraycopy(fileNameBytes, 0, combined, 0, fileNameBytes.length);
                    System.arraycopy(fileBytes, 0, combined, fileNameBytes.length, fileBytes.length);

                    List<byte[]> chunks = ByteArrayUtil.chunkByteArray(combined,
                            Configurations.MAX_PACKET_SIZE - Configurations.HEADER_LENGTH - Configurations.DATA_HEADER_LENGTH
                    );

                    long id = atomicLong.getAndIncrement();

                    for (int i = 0; i < chunks.size(); i++) {
                        byte[] chunk = chunks.get(i);
                        DataHeader dataHeader = new DataHeader(id, chunks.size(), i);
                        byte[] dataHeaderBytes = dataHeader.toBytes();

                        ByteBuffer bb = ByteBuffer.allocate(Configurations.HEADER_LENGTH + Configurations.DATA_HEADER_LENGTH + chunk.length);
                        bb.put(Configurations.HEADER_LENGTH, dataHeaderBytes);
                        bb.put(Configurations.HEADER_LENGTH + dataHeaderBytes.length, chunk);

                        byte[] headerBytes = new Header(
                                messageType.getValue(),
                                atomicInteger.getAndIncrement(),
                                Configurations.DATA_HEADER_LENGTH + chunk.length,
                                chunk
                        ).toBytes();

                        bb.put(headerBytes);

                        byte[] message = bb.array();

                        messages.add(message);
                    }
                } catch (IOException e) {
                    System.out.println("File '" + file.getPath() + "' not found.");
                }
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

                    DataHeader dataHeader = new DataHeader(messageId, chunks.size(), i);
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
