package com.pks.p2p.handlers;

import com.pks.p2p.configs.Configurations;
import com.pks.p2p.enums.MessageType;
import com.pks.p2p.protocol.DataHeader;
import com.pks.p2p.protocol.Header;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FileHandler implements PackageHandler {
    private final ConcurrentHashMap<Long, byte[][]> messages = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> firstPackageTimes = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Long> receivedFiles = new ConcurrentLinkedQueue<>();


    @Override
    public void receivePackage(@NotNull Header header, @NotNull DatagramPacket packet) {
        if (header != null && header.getMessageType() == MessageType.FILE.getValue() && packet != null) {
            byte[] data = packet.getData();
            byte[] dataHeaderBytes = new byte[Configurations.DATA_HEADER_LENGTH];

            System.arraycopy(data, Configurations.HEADER_LENGTH, dataHeaderBytes, 0, Configurations.DATA_HEADER_LENGTH);
            
            DataHeader dataHeader = DataHeader.fromBytes(dataHeaderBytes);

            byte[] message = new byte[packet.getLength() - Configurations.HEADER_LENGTH - Configurations.DATA_HEADER_LENGTH];

            System.arraycopy(data, Configurations.HEADER_LENGTH + Configurations.DATA_HEADER_LENGTH, message, 0, message.length);

            byte[][] value = messages.get(dataHeader.getId());

            if (value == null) {
                if (receivedFiles.contains(dataHeader.getId())) {
                    return;
                }
                value = new byte[dataHeader.getTotalPackages()][];
                firstPackageTimes.put(dataHeader.getId(), System.currentTimeMillis());
            }

            System.out.println("Received a fragment with sequence number " + header.getSequenceNumber());

            value[dataHeader.getPackageNumber()] = message;

            messages.put(dataHeader.getId(), value);
            run();
        }
    }

    @NotNull
    private static String getUniqueFileName(String fileName) {
        File file = new File(fileName);

        // If the file already exists, we need to modify the name
        if (!file.exists()) {
            return fileName;
        }

        // Split the filename into base name and extension
        String baseName = fileName;
        String extension = "";

        // Check if there's an extension
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            baseName = fileName.substring(0, lastDotIndex);  // Base name without extension
            extension = fileName.substring(lastDotIndex);    // File extension (including the dot)
        }

        // Now we generate a unique name by appending a number
        int i = 1;
        String uniqueFileName = baseName + "_" + i + extension;

        // Keep incrementing the number until the file doesn't exist
        while (new File(uniqueFileName).exists()) {
            i++;
            uniqueFileName = baseName + "_" + i + extension;
        }

        return uniqueFileName;
    }

    private void run() {
        messages.forEach((key, value) -> {
            int i = 0;
            while (i < value.length) {
                if (value[i] == null) {
                    break;
                }
                i++;
            }

            if (i == value.length) {
                long end = System.currentTimeMillis();

                String fileName = extractFileName(value);
                fileName = getUniqueFileName(fileName);

                File file = new File(fileName);
                value = removeFileName(value);

                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    for (byte[] bytes : value) {
                        fileOutputStream.write(bytes);
                    }
                    System.out.println("\nReceived a file: " + file.getAbsoluteFile() + " of size " + file.length() + String.format(" bytes. Transfer time: %.2f s.", ((end - firstPackageTimes.get(key)) / 1000.0)));
                    messages.remove(key);
                    firstPackageTimes.remove(key);
                    receivedFiles.add(key);
                } catch (Exception ignore) {}
            }
        });
    }


    private static byte[][] removeFileName(byte[][] fileData) {
        List<byte[]> resultChunks = new ArrayList<>();
        boolean foundNewline = false;
        int indexAfterNewline = -1;

        for (int i = 0; i < fileData.length; i++) {
            if (!foundNewline) {
                String chunkString = new String(fileData[i], StandardCharsets.UTF_8);
                int newlineIndex = chunkString.indexOf('\n');

                if (newlineIndex != -1) {
                    // Newline found: Capture the remaining part of the chunk after the newline
                    foundNewline = true;
                    indexAfterNewline = newlineIndex + 1;

                    // Add the rest of the chunk after the newline
                    if (indexAfterNewline < fileData[i].length) {
                        resultChunks.add(Arrays.copyOfRange(fileData[i], indexAfterNewline, fileData[i].length));
                    }
                }
            } else {
                // Add subsequent chunks to the result
                resultChunks.add(fileData[i]);
            }
        }

        // Convert the list back to a byte[][]
        return resultChunks.toArray(new byte[0][]);
    }

    private String extractFileName(byte[][] fileData) {
        StringBuilder fileName = new StringBuilder();

        for (byte[] bytes : fileData) {
            String chunkString = new String(bytes);
            int newLineIndex = chunkString.indexOf("\n");

            if (newLineIndex != -1) {
                fileName.append(chunkString, 0, newLineIndex);
                break;
            } else {
                fileName.append(chunkString);
            }
        }

        return fileName.toString();
    }

    public boolean hasUnreceivedFiles() {
        return !messages.isEmpty();
    }
}
