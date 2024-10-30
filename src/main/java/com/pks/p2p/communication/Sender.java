package com.pks.p2p.communication;

import com.pks.p2p.configs.Configurations;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageTypes;
import com.pks.p2p.protocol.Header;
import com.pks.p2p.util.ByteArrayUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Sender {

    private static final AtomicInteger atomicInteger = new AtomicInteger(3);

    public static void sendData(DatagramSocket socket, String data) {
        byte[] bytes = data.getBytes();
        List<byte[]> chunkedData = ByteArrayUtil.chunkByteArray(bytes, Configurations.MAX_PACKET_SIZE - Configurations.HEADER_LENGTH);
        for (byte[] chunkedDatum : chunkedData) {
            byte[] headerBytes = new Header(MessageTypes.DATA.getValue(), atomicInteger.getAndIncrement(), chunkedDatum.length, bytes).toBytes();
            ByteBuffer bb = ByteBuffer.allocate(headerBytes.length + chunkedDatum.length);
            bb.put(headerBytes);
            bb.put(chunkedDatum);

            byte[] message = bb.array();
            try {
                socket.send(new DatagramPacket(message, message.length, Connection.getAddress(), Connection.getPort()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
