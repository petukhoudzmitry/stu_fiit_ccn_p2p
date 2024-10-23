package com.pks.p2p.connection;

import com.pks.p2p.enums.MessageTypes;
import com.pks.p2p.enums.StringConstants;
import com.pks.p2p.protocol.Header;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import static com.pks.p2p.util.InputReaderUtil.readInput;
import static com.pks.p2p.configs.Configurations.INPUT_TIMEOUT_SECONDS;

public class Connection {

    private static volatile boolean connected = false;
    private static volatile boolean isListening = false;
    private static volatile InetAddress address;
    private static volatile int port;
    private static volatile boolean running = false;



    private static volatile boolean synSent = false;

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

                        System.out.println(packet.getLength());

                        int counter = 0;
                        while (packet.getLength() >= buffer.length) {
                            socket.receive(packet);
                            if (counter == 0) {
                                for(int i = 20; i < packet.getLength(); i++) {
                                    System.out.print((char) buffer[i]);
                                }
                                counter += 1;
                            } else {
                                System.out.print(new String(buffer));
                            }
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

    public static void handshake(DatagramSocket socket) {
        Thread sendSyn = sendSyn(socket);
        Thread listenToHandshakeThread = listenHandshake(socket);

        sendSyn.start();
        listenToHandshakeThread.start();

        while(true) {
            if(getConnected()) {
                sendSyn.interrupt();
                listenToHandshakeThread.interrupt();
                break;
            }
        }
    }

    private static Thread listenHandshake(DatagramSocket socket) {
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
            if (!getSynSent()) {
                try (Scanner sc = new Scanner(System.in)) {
                    System.out.println("Enter the IP address of the peer:");
                    String ip = readInput(INPUT_TIMEOUT_SECONDS);
                    while (!ip.matches(StringConstants.IP_PATTERN.getValue())) {
                        System.out.println("Wrong IP address format. Please enter a valid IP address:");
                        ip = readInput(INPUT_TIMEOUT_SECONDS);
                    }
                    System.out.println("Enter the port of the peer:");

                    String port = readInput(INPUT_TIMEOUT_SECONDS);

                    while (!port.matches(StringConstants.PORT_PATTERN.getValue())) {
                        System.out.println("Wrong port format. Please enter a valid port:");
                        port = readInput(INPUT_TIMEOUT_SECONDS);
                    }

                    Header header = new Header(MessageTypes.SYN.getValue(), 0);
                    byte[] synPacket = header.toBytes();

                    address = InetAddress.getByName(ip);

                    DatagramPacket synRequest = new DatagramPacket(synPacket, synPacket.length, address, Integer.parseInt(port));
                    socket.send(synRequest);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private synchronized static boolean getSynSent() {
        return synSent;
    }

    synchronized public static boolean getConnected() {
        return connected;
    }

    synchronized private static void setConnected(boolean value) {
        connected = value;
    }

    synchronized public static boolean getRunning() {
        return running;
    }

    synchronized public static void setRunning(boolean running) {
        Connection.running = running;
    }
}
