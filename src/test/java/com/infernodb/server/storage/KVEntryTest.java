package com.infernodb.server.storage;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class KVEntryTest {

    @Test
    void fromBuffer() {
        var key = "testKey".getBytes();
        var value = "testValue".getBytes();
        var kvEntry = KVEntry.fromKeyValue(key, value);
        var buffer = ByteBuffer.allocate(kvEntry.size());
        kvEntry.populateBuffer(buffer);
        buffer.flip();

        var readEntry = KVEntry.fromBuffer(buffer);

        assertEquals(kvEntry.timestamp(), readEntry.timestamp());
        assertEquals(kvEntry.keySize(), readEntry.keySize());
        assertEquals(kvEntry.valueSize(), readEntry.valueSize());
        assertArrayEquals(kvEntry.key(), readEntry.key());
        assertArrayEquals(kvEntry.value(), readEntry.value());
    }

    @Test
    void fromKeyValue() {
        var key = "testKey".getBytes();
        var value = "testValue".getBytes();
        var kvEntry = KVEntry.fromKeyValue(key, value);

        assertNotNull(kvEntry);
        assertEquals(key.length, kvEntry.keySize());
        assertEquals(value.length, kvEntry.valueSize());
        assertArrayEquals(key, kvEntry.key());
        assertArrayEquals(value, kvEntry.value());
    }

    @Test
    void size() {
        var key = "testKey".getBytes();
        var value = "testValue".getBytes();
        var kvEntry = KVEntry.fromKeyValue(key, value);

        assertEquals(Entry.CRC_SIZE + 8 + 4 + 4 + key.length + value.length, kvEntry.size());
    }

    @Test
    void populateBuffer() {
        var key = "testKey".getBytes();
        var value = "testValue".getBytes();
        var kvEntry = KVEntry.fromKeyValue(key, value);
        var buffer = ByteBuffer.allocate(kvEntry.size());

        // skip CRC
        buffer.position(Entry.CRC_SIZE);
        kvEntry.populateBuffer(buffer);

        assertEquals(kvEntry.size(), buffer.position());
    }

    @Test
    void timestamp() {
        var key = "testKey".getBytes();
        var value = "testValue".getBytes();
        var kvEntry = KVEntry.fromKeyValue(key, value);

        assertTrue(kvEntry.timestamp() > 0);
    }

    @Test
    void keySize() {
        var key = "testKey".getBytes();
        var value = "testValue".getBytes();
        var kvEntry = KVEntry.fromKeyValue(key, value);

        assertEquals(key.length, kvEntry.keySize());
    }

    @Test
    void valueSize() {
        var key = "testKey".getBytes();
        var value = "testValue".getBytes();
        var kvEntry = KVEntry.fromKeyValue(key, value);

        assertEquals(value.length, kvEntry.valueSize());
    }

    @Test
    void key() {
        var key = "testKey".getBytes();
        var value = "testValue".getBytes();
        var kvEntry = KVEntry.fromKeyValue(key, value);

        assertArrayEquals(key, kvEntry.key());
    }

    @Test
    void value() {
        var key = "testKey".getBytes();
        var value = "testValue".getBytes();
        var kvEntry = KVEntry.fromKeyValue(key, value);

        assertArrayEquals(value, kvEntry.value());
    }
}