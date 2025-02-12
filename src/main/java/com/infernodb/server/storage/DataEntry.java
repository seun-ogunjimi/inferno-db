package com.infernodb.server.storage;

import com.infernodb.server.storage.io.KVUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a data entry in the key-value store.
 *
 * @param id
 * @param timestamp
 * @param dataCount
 * @param dataSizes
 * @param data
 */
public record DataEntry(byte[] id, long timestamp, int dataCount, int[] dataSizes, byte[][] data) implements Entry {

    /**
     * Creates a new data entry from a buffer.
     *
     * @param buffer
     * @return
     */
    public static DataEntry fromBuffer(ByteBuffer buffer) {
        long timestamp = buffer.getLong();
        int idSize = buffer.getInt();
        int dataLength = buffer.getInt();
        int[] dataSizes = new int[dataLength];
        for (int i = 0; i < dataLength; i++) {
            dataSizes[i] = buffer.getInt();
        }
        byte[] idBytes = new byte[idSize];
        buffer.get(idBytes);
        byte[][] data = new byte[dataLength][];
        for (int i = 0; i < dataLength; i++) {
            data[i] = new byte[dataSizes[i]];
            buffer.get(data[i]);
        }
        return new DataEntry(idBytes, timestamp, dataLength, dataSizes, data);
    }

    /**
     * Creates a new data entry from a list of data.
     *
     * @param data
     * @return
     */
    public static DataEntry fromData(List<byte[]> data) {
        byte[] id = KVUtils.createId().getBytes();
        return fromData(data, id);
    }

    /**
     * Creates a new data entry from a list of data and an id.
     *
     * @param data
     * @param id
     * @return
     */
    public static DataEntry fromData(List<byte[]> data, byte[] id) {
        int dataLength = data.size();
        int[] dataSizes = data.stream().mapToInt(a -> a.length).toArray();
        return new DataEntry(id, System.currentTimeMillis(), dataLength, dataSizes, data.toArray(new byte[0][]));
    }

    /**
     * Returns the size of the data entry.
     *
     * @return size
     */
    public int size() {
        // crc + timestamp + idSize+ dataArrayLength + dataSizes.length + id + data
        return Entry.CRC_SIZE + 8 + 4 + 4 + (dataSizes.length * 4) + id.length + Arrays.stream(data).mapToInt(a -> a.length).sum();
    }

    /**
     * Populates the buffer with the data entry.
     *
     * @param buffer
     */
    @Override
    public void populateBuffer(ByteBuffer buffer) {
        if (buffer.remaining() < size()) {
            throw new IllegalArgumentException("Buffer does not have enough space to store the data entry");
        }
        populate(buffer);
    }

    /**
     * Populates the buffer with the data entry.
     *
     * @param buffer
     */
    private void populate(ByteBuffer buffer) {
        buffer.putLong(timestamp);
        buffer.putInt(id.length);
        buffer.putInt(dataCount);
        for (int i = 0; i < dataCount; i++) {
            buffer.putInt(dataSizes[i]);
        }
        buffer.put(id);
        for (int i = 0; i < dataCount; i++) {
            buffer.put(data[i]);
        }
    }
}
