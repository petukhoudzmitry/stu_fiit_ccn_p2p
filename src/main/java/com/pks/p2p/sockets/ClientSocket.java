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
            this.socket.setSoTimeout(0);
            this.socket.setReceiveBufferSize(Integer.MAX_VALUE);
            this.socket.setSendBufferSize(Integer.MAX_VALUE);
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
    }

    public void close() {
        socket.close();
    }

    public void receive(DatagramPacket packet) {
        try{
            socket.receive(packet);
        } catch (IOException ignore) {}
    }

    public synchronized int getPort() {
        return socket.getLocalPort();
    }

    public void send(DatagramPacket packet) {
        try {
            socket.send(packet);
        } catch (IOException ignore) {}
    }
}