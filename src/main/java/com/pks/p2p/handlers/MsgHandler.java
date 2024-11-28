package com.pks.p2p.handlers;

import com.pks.p2p.configs.Configurations;
import com.pks.p2p.enums.MessageType;
import com.pks.p2p.protocol.DataHeader;
import com.pks.p2p.protocol.Header;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.net.DatagramPacket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MsgHandler implements PackageHandler {

    private final ConcurrentHashMap<Long, Pair<Integer, byte[][]>> messages = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Long> receivedMessages = new ConcurrentLinkedQueue<>();


    @Override
    public void receivePackage(@NotNull Header header, @NotNull DatagramPacket packet) {

        if (header != null && header.getMessageType() == MessageType.MSG.getValue() && packet != null) {
            byte[] data = packet.getData();
            byte[] dataHeaderBytes = new byte[Configurations.DATA_HEADER_LENGTH];

            System.arraycopy(data, Configurations.HEADER_LENGTH, dataHeaderBytes, 0, Configurations.DATA_HEADER_LENGTH);

            DataHeader dataHeader = DataHeader.fromBytes(dataHeaderBytes);

            byte[] message = new byte[header.getLength() - Configurations.DATA_HEADER_LENGTH];

            System.arraycopy(data, Configurations.HEADER_LENGTH + Configurations.DATA_HEADER_LENGTH, message, 0, message.length);

            Pair<Integer, byte[][]> value = messages.get(dataHeader.getId());

            if (value == null) {
                if (receivedMessages.contains(dataHeader.getId())) {
                    return;
                }
                value = new Pair<>(
                                dataHeader.getTotalPackages(),
                                new byte[dataHeader.getTotalPackages()][]
                        );
            }

            System.out.println("Received a fragment with sequence number " + header.getSequenceNumber());

            value.getSecond()[dataHeader.getPackageNumber()] = message;
            messages.put(dataHeader.getId(), value);

            run();
        }
    }

    private void run() {
        messages.forEach((key, value) -> {
            int i = 0;
            for (; i < value.getSecond().length; i++) {
                if (value.getSecond()[i] == null) {
                    break;
                }
            }

            if (value.getFirst() == i) {
                StringBuilder message = new StringBuilder("\nReceived a message: ");

                int dataBytes = 0;
                int headerBytes = (Configurations.HEADER_LENGTH + Configurations.DATA_HEADER_LENGTH) * value.getSecond().length;

                for (byte[] data : value.getSecond()) {
                    dataBytes += data.length;
                    message.append(new String(data));
                }

                System.out.println(message + "\n");

                System.out.println("Received " + headerBytes + " bytes of header data. Percentage of header data: " + String.format("%.2f", (100. * headerBytes / (headerBytes + dataBytes))) + "%");

                messages.remove(key);
                receivedMessages.add(key);
            }
        });
    }

    public boolean hasUnreceivedMessages() {
        return !messages.isEmpty();
    }
}
