package com.pks.p2p.communication;

import com.pks.p2p.configs.Configurations;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageType;
import com.pks.p2p.protocol.DataHeader;
import com.pks.p2p.protocol.Header;
import com.pks.p2p.util.ByteArrayUtil;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Sender {

    private final Connection connection;
    private final AtomicInteger atomicInteger = new AtomicInteger(0);
    private final AtomicLong atomicLong = new AtomicLong(0L);

    private final ConcurrentLinkedDeque<Pair<Boolean, byte[]>> messages = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<Integer, Pair<Long, byte[]>> unconfirmedPackages = new ConcurrentHashMap<>();

    public Sender(Connection connection) {
        this.connection = connection;
    }

    public void startSending() {
        new Thread(() -> {
            while (connection.getConnected()) {
                if (!messages.isEmpty() && unconfirmedPackages.size() <= Configurations.WINDOW_SIZE) {
                    Pair<Boolean, byte[]> pair = messages.poll();
                    byte[] message = pair.getSecond();

                    MessageType messageType = MessageType.fromInt(Header.fromBytes(message).getMessageType());
                    if (messageType == MessageType.MSG || messageType == MessageType.FILE || messageType == MessageType.KEEP_ALIVE) {
                        addUnconfirmedPackage(message);
                    }

                    if (pair.getFirst()) {
                        byte[] corruptedMessage = new byte[message.length];
                        System.arraycopy(message, 0, corruptedMessage, 0, message.length);
                        corruptedMessage[Configurations.HEADER_LENGTH + 1] = (byte) (corruptedMessage[Configurations.HEADER_LENGTH + 1] + 1);
                        send(corruptedMessage);
                    } else {
                        send(message);
                    }

                }
            }
        }).start();
        runArq();
    }

    public void send(@NotNull byte[] data) {
        if (data != null) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            connection.getSocket().send(buffer, new InetSocketAddress(connection.getAddress(), connection.getPort()));
        }
    }

    private void addUnconfirmedPackage(@NotNull byte[] data) {
        if (data != null && data.length >= Configurations.HEADER_LENGTH) {
            Header header = Header.fromBytes(data);
            unconfirmedPackages.put(header.getSequenceNumber(), new Pair<>(System.currentTimeMillis(), data));
        }
    }

    public void confirmPackage(int sequenceNumber) {
        unconfirmedPackages.remove(sequenceNumber);
    }

    public void resendMessage(int sequenceNumber) {
        Pair<Long, byte[]> pair = unconfirmedPackages.get(sequenceNumber);
        if (pair != null) {
            send(pair.getSecond());
        }
    }

    public void send(@NotNull MessageType messageType, @NotNull String data, boolean corrupted) {
        if (data == null) {
            return;
        }

        if (messageType == MessageType.MSG) {
            sendMessage(messageType, data, corrupted);
            return;
        } else if (messageType == MessageType.FILE){
            sendFile(messageType, data, corrupted);
            return;
        } else if (messageType == MessageType.FIN_ACK) {
            messages.add(new Pair<>(corrupted, new Header(messageType.getValue(), atomicInteger.getAndIncrement(), 0, new byte[0]).toBytes()));
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
                case SYN, SYN_ACK -> send(message);
                case KEEP_ALIVE, ACK -> messages.addFirst(new Pair<>(corrupted, message));
                default -> messages.add(new Pair<>(corrupted, message));
            }
        }).start();
    }

    private void sendFile(@NotNull MessageType messageType, @NotNull String data, boolean corrupted) {
        if (data == null || data.isEmpty()) {
            return;
        }

        if (messageType == MessageType.FILE){
            String filePath = data.replace("file:", "");
            File file = new File(filePath);

            if (!file.exists()) {
                System.out.println("File '" + file.getPath() + "' not found.");
                return;
            }

            byte[] fileNameBytes = (file.getName() + "\n").getBytes(StandardCharsets.UTF_8);

            long totalBytesToSent = fileNameBytes.length + file.length();
            int lastChunkSize = (int) (totalBytesToSent % Configurations.MAX_PACKET_SIZE);

            int headerLength = Configurations.HEADER_LENGTH + Configurations.DATA_HEADER_LENGTH;
            int packetsAmount = (int)Math.ceil(1. * totalBytesToSent / Configurations.MAX_PACKET_SIZE);
            int headerBytesAmount = headerLength * packetsAmount;

            System.out.println("\nSending the file: " + file.getName() + " of size " +  totalBytesToSent + " bytes, with " + packetsAmount + " fragments of size " + Configurations.MAX_PACKET_SIZE + " bytes" + (lastChunkSize > 0 ? ", last fragment size is " + lastChunkSize + " bytes" : "."));
            System.out.println("Sent " + headerBytesAmount + " bytes of header data. Percentage of header data: " + String.format("%.2f", 100. * headerBytesAmount / (headerBytesAmount + totalBytesToSent)) + "%");

            new Thread(() -> {
                try {
                    byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));

                    // Combine filename and file content
                    byte[] combined = new byte[fileNameBytes.length + fileBytes.length];
                    System.arraycopy(fileNameBytes, 0, combined, 0, fileNameBytes.length);
                    System.arraycopy(fileBytes, 0, combined, fileNameBytes.length, fileBytes.length);

                    List<byte[]> chunks = ByteArrayUtil.chunkByteArray(combined,
                            Configurations.MAX_PACKET_SIZE
                    );


                    long id = atomicLong.getAndIncrement();

                    for (int i = 0; i < chunks.size(); i++) {
                        byte[] chunk = chunks.get(i);
                        DataHeader dataHeader = new DataHeader(id, chunks.size(), i);
                        byte[] dataHeaderBytes = dataHeader.toBytes();

                        byte[] combinedData = new byte[dataHeaderBytes.length + chunk.length];
                        System.arraycopy(dataHeaderBytes, 0, combinedData, 0, dataHeaderBytes.length);
                        System.arraycopy(chunk, 0, combinedData, dataHeaderBytes.length, chunk.length);

                        ByteBuffer bb = ByteBuffer.allocate(Configurations.HEADER_LENGTH + Configurations.DATA_HEADER_LENGTH + chunk.length);
                        bb.put(Configurations.HEADER_LENGTH, dataHeaderBytes);
                        bb.put(Configurations.HEADER_LENGTH + dataHeaderBytes.length, chunk);

                        byte[] headerBytes = new Header(
                                messageType.getValue(),
                                atomicInteger.getAndIncrement(),
                                Configurations.DATA_HEADER_LENGTH + chunk.length,
                                combinedData
                        ).toBytes();

                        bb.put(headerBytes);

                        byte[] message = bb.array();

                        messages.add(new Pair<>(corrupted, message));
                    }
                } catch (IOException e) {
                    System.out.println("File '" + file.getPath() + "' not found.");
                }
            }).start();
        }
    }


    private void sendMessage(@NotNull MessageType messageType, @NotNull String data, boolean corrupted) {
        if (data == null || data.isEmpty()) {
            return;
        }

        if (messageType == MessageType.MSG) {
            int totalBytesToSent = data.getBytes().length;
            int lastChunkSize = totalBytesToSent % Configurations.MAX_PACKET_SIZE;

            int headerLength = Configurations.HEADER_LENGTH + Configurations.DATA_HEADER_LENGTH;
            int packetsAmount = (int)Math.ceil(1. * totalBytesToSent / Configurations.MAX_PACKET_SIZE);
            int headerBytesAmount = headerLength * packetsAmount;

            System.out.println("\nSending the message of size " +  totalBytesToSent + " bytes, with " + (int)Math.ceil(1. * totalBytesToSent / Configurations.MAX_PACKET_SIZE) + " fragments of size " + Configurations.MAX_PACKET_SIZE + " bytes" + (lastChunkSize > 0 ? ", last fragment size is " + lastChunkSize + " bytes" : "."));
            System.out.println("Sent " + headerBytesAmount + " bytes of header data. Percentage of header data: " + String.format("%.2f", 100. * headerBytesAmount / (headerBytesAmount + totalBytesToSent)) + "%");

            new Thread(() -> {
                List<byte[]> chunks = ByteArrayUtil.chunkByteArray(data.getBytes(),
                        Configurations.MAX_PACKET_SIZE
                );

                long messageId = atomicLong.getAndIncrement();

                for (int i = 0; i < chunks.size(); i++) {
                    byte[] chunk = chunks.get(i);
                    ByteBuffer bb = ByteBuffer.allocate(headerLength + chunk.length);

                    DataHeader dataHeader = new DataHeader(messageId, chunks.size(), i);

                    byte[] dataHeaderBytes = dataHeader.toBytes();

                    bb.put(Configurations.HEADER_LENGTH, dataHeaderBytes);
                    bb.put(headerLength, chunk);

                    byte[] combinedData = new byte[dataHeaderBytes.length + chunk.length];
                    System.arraycopy(dataHeaderBytes, 0, combinedData, 0, dataHeaderBytes.length);
                    System.arraycopy(chunk, 0, combinedData, dataHeaderBytes.length, chunk.length);

                    byte[] headerBytes = new Header(
                            messageType.getValue(),
                            atomicInteger.getAndIncrement(),
                            dataHeader.toBytes().length + chunk.length,
                            combinedData
                    ).toBytes();

                    bb.put(headerBytes);

                    byte[] message = bb.array();

                    messages.add(new Pair<>(corrupted, message));
                }
            }).start();
        }
    }

    public synchronized boolean isSending() {
        return !messages.isEmpty();
    }

    private void runArq() {
        new Thread(() -> {
            while (connection.getConnected()) {
                unconfirmedPackages.forEach((key, value) -> {
                    if (System.currentTimeMillis() - value.getFirst() > Configurations.ARQ_TIMEOUT) {
                        send(value.getSecond());
                    }
                });
            }
        }).start();
    }
}
