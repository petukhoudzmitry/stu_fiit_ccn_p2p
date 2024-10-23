package com.pks.p2p.protocol;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.zip.CRC32;

public class Header {

    private final int messageType;
    private final int sequenceNumber;
    private final int length;
    private final long checksum;


    public Header(int messageType, int sequenceNumber, int length, byte[] data) {
        this.messageType = messageType;
        this.sequenceNumber = sequenceNumber;
        this.length = length;
        this.checksum = calculateChecksum(data);
    }

    public Header(int messageType, int sequenceNumber) {
        this.messageType = messageType;
        this.sequenceNumber = sequenceNumber;
        this.length = 0;
        this.checksum = 0L;
    }

    private long calculateChecksum(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.putInt(messageType);
        buffer.putInt(sequenceNumber);
        buffer.putInt(length);
        buffer.putLong(checksum);
        return buffer.array();
    }

    public static Header fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int messageType = buffer.getInt();
        int sequenceNumber = buffer.getInt();
        int length = buffer.getInt();
        return new Header(messageType, sequenceNumber, length, new byte[0]);
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
