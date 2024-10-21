package com.pks.p2p;

import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageTypes;
import com.pks.p2p.enums.StringConstants;
import com.pks.p2p.protocol.Header;
import com.pks.p2p.sockets.ClientSocket;
import com.pks.p2p.util.IPUtil;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Scanner;

public class Main {

    private static DatagramSocket socket = null;

    public static void main(String[] args) throws Exception {

        System.out.println("Your IP address is: " + IPUtil.getIP());

        if (args.length > 0){
            if ((args.length & 1) == 0) {
                for (int i = 0; i < args.length; i+=2) {
                    if (args[i].equals("-p")) {
                        socket = ClientSocket.getInstance(Integer.parseInt(args[i+1]));
                    }
                }
            }
        }

        if (socket == null) {
            socket = ClientSocket.getInstance(new Random().nextInt(49152, 65536));
        }

        System.out.println("Socket is bound to port " + socket.getLocalPort());

        Scanner sc = new Scanner(System.in);

        label:
        while(true) {
            System.out.println("Enter 'listen' to listen for incoming connections, 'connect' to connect to a socket or 'exit' to close the socket.");
            String line = sc.nextLine();
            switch (line) {
                case "exit":
                    System.out.println("Closing socket...");
                    Connection.setRunning(false);
                    socket.close();
                    return;

                case "listen":
                    Connection.listen(socket);
                    break label;
                case "connect":
                    System.out.println("Enter the IP address of the peer:");
                    String ip = sc.nextLine();
                    if (ip.matches(StringConstants.IP_PATTERN.getValue())) {
                        System.out.println("Enter the port number of the peer:");
                        String port = sc.nextLine();
                        if (port.trim().matches("\\d+") && Integer.parseInt(port) > 0 && Integer.parseInt(port) < 65536) {
                            Connection.handshake(socket, ip, Integer.parseInt(port));
                        } else {
                            System.out.println("Invalid port number.");
                        }
                    } else {
                        System.out.println("Invalid IP address.");
                    }
                    break label;
            }
        }

        System.out.println("Waiting for connection...");
        int i = 0;
        while(!Connection.getConnected() && i++ < 30) {
            Thread.sleep(1000);
        }

        if(!Connection.getConnected()) {
            socket.close();
            return;
        }

        if(!Connection.isIsListening()) {
            Connection.listen(socket);
        }

        while(true) {
            System.out.println("Enter a message to send to the peer or '\\exit' to close the socket:");
            String message = sc.nextLine();
            if(message.equals("\\exit")) {
                break;
            }
            byte[] buffer = message.getBytes();

            Header header = new Header(MessageTypes.DATA.getValue(), 0, buffer.length, buffer);
            ByteBuffer combined = ByteBuffer.allocate(header.toBytes().length + buffer.length);

            combined.put(header.toBytes());
            combined.put(buffer);

            DatagramPacket packet = new DatagramPacket(combined.array(), combined.array().length, Connection.getAddress(), Connection.getPort());
            socket.send(packet);
        }


        System.out.println("Closing socket...");
        Connection.setRunning(false);
        socket.close();
    }
}