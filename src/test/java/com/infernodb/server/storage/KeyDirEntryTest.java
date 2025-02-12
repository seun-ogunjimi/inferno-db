package com.infernodb.server.storage;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyDirEntryTest {

    @Test
    void testFromBuffer() {
        var buffer = ByteBuffer.allocate(1024);
        buffer.putLong(1627846261L); // timestamp
        buffer.putInt(3); // key size
        buffer.putInt(5); // value size
        buffer.put("key".getBytes()); // key
        buffer.put("value".getBytes()); // value
        buffer.flip();

        var entry = KeyDirEntry.fromBuffer(buffer, "file1");
        assertEquals("file1", entry.fileId());
        assertArrayEquals("key".getBytes(), entry.key());
        assertEquals(5, entry.valueSize());
        assertEquals(1627846261L, entry.timestamp());
    }

    @Test
    void testFromKVEntry() {
        var kvEntry = KVEntry.fromKeyValue("key".getBytes(), "value".getBytes());
        var entry = KeyDirEntry.fromKVEntry(kvEntry, "file1", 100L);

        assertEquals("file1", entry.fileId());
        assertArrayEquals("key".getBytes(), entry.key());
        assertEquals(5, entry.valueSize());
        assertEquals(100L, entry.valuePosition());
        assertEquals(kvEntry.timestamp(), entry.timestamp());
    }

    @Test
    void testPosition() {
        var kvEntry = KVEntry.fromKeyValue("key".getBytes(), "value".getBytes());
        var entry = KeyDirEntry.fromKVEntry(kvEntry, "file1", 100L);

        assertEquals(100L - "key".getBytes().length - 4 - 4 - 8 - Entry.CRC_SIZE, entry.position());
    }

    @Test
    void testSize() {
        var kvEntry = KVEntry.fromKeyValue("key".getBytes(), "value".getBytes());
        var entry = KeyDirEntry.fromKVEntry(kvEntry, "file1", 100L);

        assertEquals(Entry.CRC_SIZE + 8 + 4 + 4 + "key".getBytes().length + 5, entry.size());
    }
}