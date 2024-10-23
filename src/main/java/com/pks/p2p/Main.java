package com.pks.p2p;

import com.pks.p2p.connection.Connection;
import com.pks.p2p.sockets.ClientSocket;
import com.pks.p2p.util.IPUtil;

import java.net.*;
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
                    Connection.handshake(socket);
                    break label;
            }
        }

        System.out.println("Closing socket...");
        socket.close();
    }
}