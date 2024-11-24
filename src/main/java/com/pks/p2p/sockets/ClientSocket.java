package com.pks.p2p.sockets;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;


public class ClientSocket {

    private volatile DatagramChannel channel = null;

    public ClientSocket(int port) {
        setChannel(port);
    }

    public synchronized DatagramChannel getChannel() {
        return channel;
    }

    private synchronized void setChannel(int port) {
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SocketAddress receive(ByteBuffer buffer) {
        try{
            return channel.receive(buffer);
        } catch (IOException ignore) {}
        return null;
    }

    public synchronized int getPort() {
        return channel.socket().getLocalPort();
    }

    public void connect(InetAddress address, int port) {
        try {
            channel.connect(new InetSocketAddress(address, port));
        } catch (IOException ignore) {}
    }

    public void send(ByteBuffer buffer, InetSocketAddress address) {
        try {
            channel.send(buffer, address);
        } catch (IOException ignore) {}
    }
}