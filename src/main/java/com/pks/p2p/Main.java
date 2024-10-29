package com.pks.p2p;

import com.pks.p2p.connection.Connection;
import com.pks.p2p.sockets.ClientSocket;
import com.pks.p2p.util.IPUtil;
import com.pks.p2p.util.InputReaderUtil;

import java.net.*;
import java.util.Random;

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

        label:
        while(!Connection.getConnected()) {
            System.out.println("Enter 'connect' to connect to a peer or 'exit' to close the program.");
            String line = InputReaderUtil.readInput(System.in, () -> true);
            line = line == null ? "" : line;
            switch (line) {
                case "exit":
                    Connection.setRunning(false);
                    break label;
                case "connect":
                    Connection.handshake(socket);
                    break label;
            }
        }

        while(Connection.getConnected()) {
            System.out.println("Enter a message to send to the peer or ':exit!' to close the program.");
            String line = InputReaderUtil.readInput(System.in, () -> true);
            if (":exit!".equals(line)) {
                Connection.setRunning(false);
                break;
            }
            Connection.sendData(socket, line);
        }

        System.out.println("Closing socket...");
        socket.close();
        System.in.close();
    }
}