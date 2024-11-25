package com.pks.p2p.handlers;

import com.pks.p2p.communication.Sender;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageType;
import com.pks.p2p.protocol.Header;
import org.jetbrains.annotations.NotNull;

import java.net.DatagramPacket;

public class FinHandler implements PackageHandler {

    private final Connection connection;
    private final Sender sender;

    public FinHandler(Connection connection, Sender sender) {
        this.connection = connection;
        this.sender = sender;
    }

    @Override
    public void receivePackage(@NotNull Header header, @NotNull DatagramPacket packet) {
        if (header.getMessageType() == MessageType.FIN.getValue()) {
            while (sender.isSending());
            sender.send(MessageType.FIN_ACK, "", false);
            while(sender.isSending());
            connection.setConnected(false);
        } else if (header.getMessageType() == MessageType.FIN_ACK.getValue()) {
            connection.setConnected(false);
        }
    }
}
