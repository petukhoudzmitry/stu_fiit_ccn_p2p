package com.pks.p2p.communication;

import com.pks.p2p.configs.Configurations;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageTypes;
import com.pks.p2p.protocol.Header;
import com.pks.p2p.util.Checksum;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class Receiver {
    public static void listen(DatagramSocket socket) {
        new Thread(() -> {
            try{
                while(Connection.getConnected()) {
                    byte[] buffer = new byte[Configurations.MAX_PACKET_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    try {
                        socket.receive(packet);
                    } catch (SocketException e) {
                        break;
                    }

                    if(!packet.getAddress().equals(Connection.getAddress()) || packet.getPort() != Connection.getPort()) {
                        System.out.println("Received a packet from an unknown source. Ignoring...");
                        continue;
                    }

                    byte[] data = packet.getData();
                    Header header = Header.fromBytes(data);
                    int messageType = header.getMessageType();



                    if(messageType == MessageTypes.DATA.getValue() && Connection.getConnected()) {
                        byte[] message = new byte[header.getLength()];
                        System.arraycopy(data, Configurations.HEADER_LENGTH, message, 0, message.length);

                        System.out.println(new String(message));
                    }
                }
                socket.close();
            } catch(IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    public static boolean isCorrupted(byte[] packet) {
        if (packet == null) return false;

        Header header = Header.fromBytes(packet);
        ByteBuffer byteBuffer = ByteBuffer.allocate(Configurations.HEADER_LENGTH - 8 + packet.length);
        byteBuffer.putInt(header.getMessageType()).putInt(header.getSequenceNumber()).putInt(header.getLength());

        byte[] data = new byte[header.getLength()];
        System.arraycopy(packet, 0, data, 0, data.length);
        byteBuffer.put(data);

        return header.getChecksum() == Checksum.calculateChecksum(byteBuffer.array());
    }
}
