package com.pks.p2p.communication;

import com.pks.p2p.configs.Configurations;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageType;
import com.pks.p2p.protocol.Header;
import com.pks.p2p.util.ByteArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Sender {

    private final Connection connection;
    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    public Sender(Connection connection) {
        this.connection = connection;
    }


    public void sendData(@NotNull MessageType messageType, @NotNull String data) {
        byte[] bytes = data.getBytes();
        List<byte[]> chunks = !data.isEmpty() ? ByteArrayUtil.chunkByteArray(bytes, Configurations.MAX_PACKET_SIZE - Configurations.HEADER_LENGTH) : List.of(new byte[]{});

        for (byte[] chunkedData : chunks) {
            byte[] headerBytes = new Header(messageType.getValue(), atomicInteger.getAndIncrement(), chunkedData.length, bytes).toBytes();
            ByteBuffer bb = ByteBuffer.allocate(headerBytes.length + chunkedData.length);
            bb.put(headerBytes);
            bb.put(chunkedData);

            byte[] message = bb.array();

            connection.getSocket().send(new DatagramPacket(message, message.length, connection.getAddress(), connection.getPort()));
        }
    }
}
