package com.infernodb.server.storage;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Represents an entry for data storage.
 * <p>
 * Represents a key-value entry in the key-value store.
 * <p>
 * The key-value entry is stored in the file as follows:
 *   <ul>
 *       <li>checksum (8 bytes)</li>
 *       <li>timestamp (8 bytes)</li>
 *       <li>key size (4 bytes)</li>
 *       <li>value size (4 bytes)</li>
 *       <li>key (key size bytes)</li>
 *       <li>value (value size bytes)</li>
 *  </ul>
 * </p>
 * <p>
 *     The checksum is calculated as follows:
 *     <ul>
 *         <li>Calculate the CRC32 checksum of the timestamp, key size, value size, key, and value</li>
 *         <li>Convert the checksum to a byte array</li>
 *         <li>Append the checksum to the beginning of the entry</li>
 *         <li>When reading the entry, calculate the checksum of the timestamp, key size, value size, key, and value</li>
 *         <li>Compare the calculated checksum with the checksum in the entry</li>
 *     </ul>
 * </p>
 */
public record KVEntry(
        long timestamp,
        int keySize,
        int valueSize,
        byte[] key,
        byte[] value
) implements Entry {

    public KVEntry {
        Objects.requireNonNull(key, "Key must not be null");
        Objects.requireNonNull(value, "Value must not be null");
        if (timestamp < 0) {
            throw new IllegalArgumentException("Timestamp must be non-negative");
        }
        if (keySize < 0) {
            throw new IllegalArgumentException("Key size must be non-negative");
        }
        if (valueSize < 0) {
            throw new IllegalArgumentException("Value size must be non-negative");
        }

        if (keySize != key.length) {
            throw new IllegalArgumentException("Key size does not match key length");
        }
        if (valueSize != value.length) {
            throw new IllegalArgumentException("Value size does not match value length");
        }
    }

    /**
     * Creates a new key-value entry from a buffer.
     *
     * @param buffer the buffer
     * @return the key-value entry
     */
    public static KVEntry fromBuffer(ByteBuffer buffer) {
        //always read/skip the CRC before calling this method
        long timestamp = buffer.getLong();
        int keySize = buffer.getInt();
        int valueSize = buffer.getInt();
        byte[] key = new byte[keySize];
        buffer.get(key);
        byte[] value = new byte[valueSize];
        buffer.get(value);
        return new KVEntry(timestamp, keySize, valueSize, key, value);
    }

    /**
     * Creates a new key-value entry from a key and a value.
     *
     * @param key   the key
     * @param value the value
     * @return the key-value entry
     */
    public static KVEntry fromKeyValue(byte[] key, byte[] value) {
        return new KVEntry(System.currentTimeMillis(), key.length, value.length, key, value);
    }

    /**
     * The size of the entry in the file.
     *
     * @return the size of the entry
     */
    public int size() {
        return Entry.CRC_SIZE + 8 + 4 + 4 + keySize + valueSize; //   timestamp + keySize + valueSize + key + value
    }

    /**
     * Populates the buffer with the entry.
     *
     * @param buffer the buffer
     */
    public void populateBuffer(ByteBuffer buffer) {
        //always write/skip the CRC before calling this method
        if (buffer.remaining() >= size() - Entry.CRC_SIZE) {
            populate(buffer);
            return;
        }
        throw new IllegalArgumentException("Buffer does not have enough space to fill the entry " + buffer.remaining() + " < " + size());
    }

    /**
     * Populates the buffer with the entry.
     *
     * @param buffer the buffer
     */
    private void populate(ByteBuffer buffer) {
        buffer.putLong(timestamp);
        buffer.putInt(keySize);
        buffer.putInt(valueSize);
        buffer.put(key);
        buffer.put(value);
    }
}
