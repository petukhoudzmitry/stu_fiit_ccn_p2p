package com.pks.p2p.sockets;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


public class ClientSocket {

    private volatile DatagramSocket socket = null;

    public ClientSocket(int port) {
        setSocket(port);
    }

    public synchronized DatagramSocket getSocket() {
        return socket;
    }

    private synchronized void setSocket(int port) {
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
    }

    public synchronized void close() {
        socket.close();
    }

    public synchronized void receive(DatagramPacket packet) {
        try{
            socket.receive(packet);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public synchronized int getPort() {
        return socket.getLocalPort();
    }

    public void send(DatagramPacket packet) {
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}