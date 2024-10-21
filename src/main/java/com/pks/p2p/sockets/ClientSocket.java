package com.pks.p2p.sockets;

import java.net.DatagramSocket;
import java.net.SocketException;


public class ClientSocket {

    private static volatile DatagramSocket socket = null;

    public static DatagramSocket getInstance(int port) {
        try {
            if(socket == null) {
                synchronized (ClientSocket.class) {
                    if(socket == null) {
                        socket = new DatagramSocket(port);
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
        return socket;
    }
}