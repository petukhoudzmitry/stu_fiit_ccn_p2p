package com.pks.p2p.handlers;

import com.pks.p2p.protocol.Header;

import java.net.DatagramPacket;

public interface PackageHandler {
    void receivePackage(Header header, DatagramPacket packet);
}
