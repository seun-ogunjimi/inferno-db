package com.infernodb.server.storage;

import java.nio.ByteBuffer;

/**
 * Represents an entry for data storage.
 */
public record KeyDirEntry(String fileId, byte[] key, int valueSize, long valuePosition, long timestamp) {
    /**
     * Creates a new key directory entry from a buffer.
     *
     * @param buffer
     * @param fileId
     * @return the key directory entry
     */
    public static KeyDirEntry fromBuffer(ByteBuffer buffer, String fileId) {
        long timestamp = buffer.getLong();
        int keySize = buffer.getInt();
        int valueSize = buffer.getInt();
        byte[] key = new byte[keySize];
        buffer.get(key);
        var valuePosition = buffer.position();
        buffer.position(buffer.position() + valueSize); // skip value bytes
        return new KeyDirEntry(fileId, key, valueSize, valuePosition, timestamp);
    }

    /**
     * Creates a new key directory entry from a KV entry.
     *
     * @param kvEntry
     * @param fileId
     * @param valuePosition
     * @return the key directory entry
     */
    public static KeyDirEntry fromKVEntry(KVEntry kvEntry, String fileId, long valuePosition) {
        var key = kvEntry.key();
        var valueSize = kvEntry.valueSize();
        var timestamp = kvEntry.timestamp();
        return new KeyDirEntry(fileId, key, valueSize, valuePosition, timestamp);
    }

    /**
     * The start position of the entry in the file.
     *
     * @return the position of the entry
     */
    public long position() {
        // //   valuePosition - key.length - valueSize - keySize - timestamp
        return valuePosition - key.length - 4 - 4 - 8 - Entry.CRC_SIZE;
    }

    /**
     * The size of the entry in the file.
     *
     * @return the size of the entry
     */
    public int size() {
        return Entry.CRC_SIZE + 8 + 4 + 4 + key.length + valueSize;
    }
}
