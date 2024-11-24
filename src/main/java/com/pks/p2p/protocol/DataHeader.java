package com.pks.p2p.protocol;

import com.pks.p2p.configs.Configurations;

import java.nio.ByteBuffer;

public class DataHeader {

    private final long id;
    private final int totalPackages;
    private final int packageNumber;

    public DataHeader(long id, int totalPackages, int packageNumber) {
        this.id = id;
        this.totalPackages = totalPackages;
        this.packageNumber = packageNumber;
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


    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(Configurations.DATA_HEADER_LENGTH);
        buffer.putLong(id).putInt(totalPackages).putInt(packageNumber);

        return buffer.array();
    }

    public static DataHeader fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long id = buffer.getLong();
        int totalPackages = buffer.getInt();
        int packageNumber = buffer.getInt();

        return new DataHeader(id, totalPackages, packageNumber);
    }

    @Override
    public String toString() {
        return "DataHeader{" +
                "id=" + id +
                ", totalPackages=" + totalPackages +
                ", packageNumber=" + packageNumber +
                '}';
    }
}
