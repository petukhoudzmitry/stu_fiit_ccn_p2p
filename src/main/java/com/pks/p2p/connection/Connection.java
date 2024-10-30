package com.pks.p2p.connection;

import com.pks.p2p.enums.MessageTypes;
import com.pks.p2p.enums.StringConstants;
import com.pks.p2p.protocol.Header;

import java.io.IOException;
import java.net.*;

import static com.pks.p2p.communication.Receiver.listen;
import static com.pks.p2p.util.UserInputUtil.getUserValue;

public class Connection {

    private static volatile boolean connected = false;
    private static volatile InetAddress address;
    private static volatile int port;

    public static void handshake(DatagramSocket socket) {
        Thread sendSyn = sendSyn(socket);
        Thread listenToHandshakeThread = listenToHandshake(socket);

        sendSyn.start();
        listenToHandshakeThread.start();

        while(true) {
            if(getConnected()) {
                sendSyn.interrupt();
                listenToHandshakeThread.interrupt();
                listen(socket);
                break;
            }
        }
    }

    private static Thread listenToHandshake(DatagramSocket socket) {
        return new Thread(() -> {
            while(!getConnected()){
                try {
                    Header synHeader = new Header(MessageTypes.SYN.getValue(), 0);
                    Header synAckHeader = new Header(MessageTypes.SYN_ACK.getValue(), 1);
                    Header ackHeader = new Header(MessageTypes.ACK.getValue(), 2);

                    byte[] buffer = new byte[synHeader.toBytes().length];

                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    Header header = Header.fromBytes(packet.getData());

                    if(header.equals(synHeader)) {
                        byte[] synAckPacket = synAckHeader.toBytes();
                        DatagramPacket synAckResponse = new DatagramPacket(synAckPacket, synAckPacket.length, packet.getAddress(), packet.getPort());
                        socket.send(synAckResponse);
                    } else if(header.equals(synAckHeader)) {
                        byte[] ackPacket = ackHeader.toBytes();
                        DatagramPacket ackResponse = new DatagramPacket(ackPacket, ackPacket.length, packet.getAddress(), packet.getPort());
                        socket.send(ackResponse);
                        address = packet.getAddress();
                        port = packet.getPort();
                        setConnected(true);
                    } else if(header.equals(ackHeader)) {
                        address = packet.getAddress();
                        port = packet.getPort();
                        setConnected(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static Thread sendSyn(DatagramSocket socket) {
        return new Thread(() -> {
            if (!getConnected()) {
                try{
                    System.out.println("Enter the IP address of the peer:");
                    long startTime = System.currentTimeMillis();
                    String ip = getUserValue(
                            System.in,
                            StringConstants.IP_PATTERN.getValue(),
                            "Wrong IP address format. Please enter a valid IP address:",
                            () -> !Connection.getConnected() && System.currentTimeMillis() - startTime < 5_000
                    );

                    if(ip == null) {
                        return;
                    }

                    System.out.println("Enter the port of the peer:");

                    String port = getUserValue(
                            System.in,
                            StringConstants.PORT_PATTERN.getValue(),
                            "Wrong port format. Please enter a valid port:",
                            () -> !Connection.getConnected()
                    );

                    if(port == null) {
                        return;
                    }

                    Header header = new Header(MessageTypes.SYN.getValue(), 0);
                    byte[] synPacket = header.toBytes();

                    address = InetAddress.getByName(ip);

                    if(!Connection.getConnected()){
                        DatagramPacket synRequest = new DatagramPacket(synPacket, synPacket.length, address, Integer.parseInt(port));
                        socket.send(synRequest);

                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    synchronized public static InetAddress getAddress() {
        return address;
    }

    synchronized public static int getPort() {
        return port;
    }

    synchronized public static boolean getConnected() {
        return connected;
    }

    synchronized private static void setConnected(boolean value) {
        connected = value;
    }
}
