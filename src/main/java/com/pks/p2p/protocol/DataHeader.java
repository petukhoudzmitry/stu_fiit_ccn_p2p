package com.pks.p2p.protocol;

import com.pks.p2p.configs.Configurations;

import java.nio.ByteBuffer;

public class DataHeader {

    private final long id;
    private final int totalPackages;
    private final int packageNumber;
    private final byte[] name;

    public DataHeader(long id, int totalPackages, int packageNumber, byte[] name) {
        this.id = id;
        this.totalPackages = totalPackages;
        this.packageNumber = packageNumber;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public int getTotalPackages() {
        return totalPackages;
    }

    public int getPackageNumber() {
        return packageNumber;
    }

    public byte[] getName() {
        return name;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(Configurations.DATA_HEADER_LENGTH + name.length);
        buffer.putLong(id).putInt(totalPackages).putInt(packageNumber).put(name);

        return buffer.array();
    }

    public static DataHeader fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long id = buffer.getLong();
        int totalPackages = buffer.getInt();
        int packageNumber = buffer.getInt();
        byte[] name = new byte[bytes.length - Configurations.DATA_HEADER_LENGTH];
        buffer.get(name);

        return new DataHeader(id, totalPackages, packageNumber, name);
    }
}
