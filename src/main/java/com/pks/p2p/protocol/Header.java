package com.pks.p2p.protocol;

import com.pks.p2p.configs.Configurations;

import java.nio.ByteBuffer;
import java.util.Objects;

import static com.pks.p2p.util.Checksum.calculateChecksum;

public class Header {

    private final int messageType;
    private final int sequenceNumber;
    private final int length;
    private final long checksum;


    public Header(int messageType, int sequenceNumber, int length, byte[] data) {
        this.messageType = messageType;
        this.sequenceNumber = sequenceNumber;
        this.length = length;
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length + Configurations.HEADER_LENGTH - 8);
        byteBuffer.putInt(this.messageType).putInt(this.sequenceNumber).putInt(this.length).put(data);
        this.checksum = calculateChecksum(byteBuffer.array());
    }

    public Header(int messageType, int sequenceNumber) {
        this.messageType = messageType;
        this.sequenceNumber = sequenceNumber;
        this.length = 0;
        ByteBuffer byteBuffer = ByteBuffer.allocate(Configurations.HEADER_LENGTH - 8);
        byteBuffer.putInt(this.messageType).putInt(this.sequenceNumber).putInt(this.length).put(new byte[0]);
        this.checksum = calculateChecksum(byteBuffer.array());
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(Configurations.HEADER_LENGTH);
        buffer.putInt(messageType).putInt(sequenceNumber).putInt(length).putLong(checksum);
        return buffer.array();
    }

    public static Header fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int messageType = buffer.getInt();
        int sequenceNumber = buffer.getInt();
        int length = buffer.getInt();
        buffer.getLong();
        byte[] data = new byte[length];
        buffer.get(data);
        return new Header(messageType, sequenceNumber, length, data);
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getLength() {
        return length;
    }

    public long getChecksum() {
        return checksum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Header header = (Header) o;
        return messageType == header.messageType && sequenceNumber == header.sequenceNumber && length == header.length && checksum == header.checksum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageType, sequenceNumber, length, checksum);
    }

    @Override
    public String toString() {
        return "Header{" +
                "messageType=" + messageType +
                ", sequenceNumber=" + sequenceNumber +
                ", length=" + length +
                ", checksum=" + checksum +
                '}';
    }
}
