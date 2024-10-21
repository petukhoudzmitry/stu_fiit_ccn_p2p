package com.pks.p2p.connection;

import com.pks.p2p.enums.MessageTypes;
import com.pks.p2p.protocol.Header;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Connection {

    private static volatile boolean connected = false;
    private static volatile boolean isListening = false;
    private static volatile InetAddress address;
    private static volatile int port;
    private static volatile boolean running = false;

    public static boolean isIsListening() {
        return isListening;
    }

    synchronized public static InetAddress getAddress() {
        return address;
    }

    synchronized public static int getPort() {
        return port;
    }

    public static void listen(DatagramSocket socket) {
        new Thread(() -> {
            try{
                isListening = true;
                running = true;
                while(running) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        socket.receive(packet);
                    } catch (SocketException e) {
                        break;
                    }

                    int messageType = Header.fromBytes(packet.getData()).getMessageType();

                    if(messageType == MessageTypes.SYN.getValue() && !connected) {
                        Header synAckHeader = new Header(MessageTypes.SYN_ACK.getValue(), 0);
                        byte[] synAckPacket = synAckHeader.toBytes();
                        DatagramPacket synAckResponse = new DatagramPacket(synAckPacket, synAckPacket.length, packet.getAddress(), packet.getPort());
                        socket.send(synAckResponse);
                    } else if(messageType == MessageTypes.ACK.getValue() && !connected) {
                        address = packet.getAddress();
                        port = packet.getPort();
                        connected = true;
                    } else if(messageType == MessageTypes.DATA.getValue() && connected) {
                        System.out.println("Received message: ");
                        for(int i = 20; i < packet.getLength(); i++) {
                            System.out.print((char) buffer[i]);
                        }
                        System.out.println();
                    }
                }
                socket.close();
            } catch(IOException e) {
                isListening = false;
                e.printStackTrace();
            }

        }).start();
    }

    public static void handshake(DatagramSocket socket, String ip, int p) {
        new Thread(() -> {
            Header header = new Header(MessageTypes.SYN.getValue(), 0);
            byte[] synPacket = header.toBytes();

            try {
                address = InetAddress.getByName(ip);
                port = p;

                DatagramPacket synRequest = new DatagramPacket(synPacket, synPacket.length, address, port);
                socket.send(synRequest);

                byte[] synAckPacket = new byte[14];
                DatagramPacket response = new DatagramPacket(synAckPacket, synAckPacket.length);
                socket.receive(response);
                Header synAckHeader = Header.fromBytes(response.getData());

                if(synAckHeader.getMessageType() == MessageTypes.SYN_ACK.getValue()) {
                    Header ackHeader = new Header(MessageTypes.ACK.getValue(), 0);
                    byte[] ackPacket = ackHeader.toBytes();
                    DatagramPacket ackRequest = new DatagramPacket(ackPacket, ackPacket.length, address, port);
                    socket.send(ackRequest);

                    connected = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    synchronized public static boolean getConnected() {
        return connected;
    }

    synchronized public static boolean getRunning() {
        return running;
    }

    synchronized public static void setRunning(boolean running) {
        Connection.running = running;
    }
}
