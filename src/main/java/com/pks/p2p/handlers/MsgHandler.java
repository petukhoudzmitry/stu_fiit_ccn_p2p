package com.pks.p2p.handlers;

import com.pks.p2p.configs.Configurations;
import com.pks.p2p.connection.Connection;
import com.pks.p2p.enums.MessageType;
import com.pks.p2p.protocol.DataHeader;
import com.pks.p2p.protocol.Header;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.net.DatagramPacket;
import java.util.concurrent.ConcurrentHashMap;

public class MsgHandler implements PackageHandler {

    private final Connection connection;

    private final ConcurrentHashMap<Long, Pair<Integer, byte[][]>> messages = new ConcurrentHashMap<>();
    private volatile boolean isRunning = false;


    public MsgHandler(Connection connection) {
        this.connection = connection;
    }



    @Override
    public void receivePackage(@NotNull Header header, @NotNull DatagramPacket packet) {

        if (header != null && header.getMessageType() == MessageType.MSG.getValue() && packet != null) {
            byte[] data = packet.getData();
            byte[] dataHeaderBytes = new byte[Configurations.DATA_HEADER_LENGTH];

            System.arraycopy(data, Configurations.HEADER_LENGTH, dataHeaderBytes, 0, Configurations.DATA_HEADER_LENGTH);

            DataHeader dataHeader = DataHeader.fromBytes(dataHeaderBytes);

            byte[] message = new byte[header.getLength() - Configurations.DATA_HEADER_LENGTH];

            System.arraycopy(data, Configurations.HEADER_LENGTH + Configurations.DATA_HEADER_LENGTH, message, 0, message.length);

            Pair<Integer, byte[][]> value = messages.getOrDefault(dataHeader.getId(),
                    new Pair<>(
                            dataHeader.getTotalPackages(),
                            new byte[dataHeader.getTotalPackages()][]
                    )
            );

            value.getSecond()[dataHeader.getPackageNumber()] = message;
            messages.put(dataHeader.getId(), value);

            run();
        }
    }

    private void run() {
        new Thread(() -> {
            messages.forEach((key, value) -> {
                int i = 0;
                for (; i < value.getSecond().length; i++) {
                    if (value.getSecond()[i] == null) {
                        break;
                    }
                }

                if (value.getFirst() == i) {
                    StringBuilder message = new StringBuilder("Received a message: ");

                    for (byte[] data : value.getSecond()) {
                        message.append(new String(data));
                    }

                    System.out.println(message);

                    messages.remove(key);
                }
            });
        }).start();
    }


    private synchronized boolean isRunning() {
        return isRunning;
    }

    private synchronized void setRunning(boolean running) {
        isRunning = running;
    }
}
