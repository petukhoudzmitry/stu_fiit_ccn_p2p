package com.pks.p2p.connection;

import com.pks.p2p.enums.MessageTypes;
import com.pks.p2p.enums.StringConstants;
import com.pks.p2p.protocol.Header;
import com.pks.p2p.util.ByteArrayUtil;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.List;

import static com.pks.p2p.util.InputReaderUtil.readInput;

public class Connection {

    private static volatile boolean connected = false;
    private static volatile InetAddress address;
    private static volatile int port;
    private static volatile boolean running = false;

    private static volatile boolean synSent = false;

    public static void listen(DatagramSocket socket) {
        new Thread(() -> {
            try{
                while(getConnected()) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    try {
                        socket.receive(packet);
                    } catch (SocketException e) {
                        break;
                    }

                    if(!packet.getAddress().equals(address) || packet.getPort() != port) {
                        System.out.println("Received a packet from an unknown source. Ignoring...");
                        continue;
                    }


                    byte[] data = packet.getData();
                    Header header = Header.fromBytes(data);
                    int messageType = header.getMessageType();

                    if(messageType == MessageTypes.DATA.getValue() && getConnected()) {
                        System.out.println("Received message: ");

                        byte[] message = new byte[header.getLength()];
                        System.arraycopy(data, 20, message, 0, message.length);

                        System.out.println(new String(message));
                    }
                }
                socket.close();
            } catch(IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    public static void sendData(DatagramSocket socket, String data) {
        byte[] bytes = data.getBytes();
        List<byte[]> chunkedData = ByteArrayUtil.chunkByteArray(bytes, 1004);
        for(int i = 0; i < chunkedData.size(); i++) {
            byte[] headerBytes = new Header(MessageTypes.DATA.getValue(), i, chunkedData.get(i).length, bytes).toBytes();
            ByteBuffer bb = ByteBuffer.allocate(headerBytes.length + chunkedData.get(i).length);
            bb.put(headerBytes);
            bb.put(chunkedData.get(i));

            byte[] message = bb.array();
            try {
                socket.send(new DatagramPacket(message, message.length, address, port));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void handshake(DatagramSocket socket) {
        Thread sendSyn = sendSyn(socket);
        Thread listenToHandshakeThread = listenToHandshake(socket);

        sendSyn.start();
        listenToHandshakeThread.start();

        while(true) {
            if(getConnected()) {
                sendSyn.interrupt();
                listenToHandshakeThread.interrupt();
                setRunning(true);
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
            if (!getSynSent()) {
                try{
                    System.out.println("Enter the IP address of the peer:");
                    String ip = readInput(System.in, () -> !Connection.getConnected());

                    if(ip == null) {
                        return;
                    }else {
                        while (!ip.matches(StringConstants.IP_PATTERN.getValue())) {
                            System.out.println("Wrong IP address format. Please enter a valid IP address:");

                            ip = readInput(System.in, () -> !Connection.getConnected());
                        }
                    }

                    System.out.println("Enter the port of the peer:");

                    String port = readInput(System.in, () -> !Connection.getConnected());

                    if(port == null) {
                        return;
                    } else {
                        while (!port.matches(StringConstants.PORT_PATTERN.getValue())) {
                            System.out.println("Wrong port format. Please enter a valid port:");
                            port = readInput(System.in, () -> !Connection.getConnected());
                        }
                    }

                    Header header = new Header(MessageTypes.SYN.getValue(), 0);
                    byte[] synPacket = header.toBytes();

                    address = InetAddress.getByName(ip);

                    if(!Connection.getConnected()){
                        DatagramPacket synRequest = new DatagramPacket(synPacket, synPacket.length, address, Integer.parseInt(port));
                        socket.send(synRequest);
                        setSynSent(true);
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

    synchronized private static void setSynSent(boolean value) {
        synSent = value;
    }

    synchronized private static boolean getSynSent() {
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
