package com.infernodb.server.storage;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataEntryTest {

    @Test
    void testFromBuffer() {
        var buffer = ByteBuffer.allocate(1024);
        var data = List.of("value1".getBytes(), "value2".getBytes());
        var id = "id1".getBytes();
        buffer.putLong(1627846261L); // timestamp
        buffer.putInt(id.length); // id size
        buffer.putInt(2); // data count
        buffer.putInt(data.getFirst().length); // data size 1
        buffer.putInt(data.get(1).length); // data size 2
        buffer.put(id); // id
        buffer.put(data.getFirst()); // data 1
        buffer.put(data.get(1)); // data 2
        buffer.flip();

        var entry = DataEntry.fromBuffer(buffer);
        assertEquals(1627846261L, entry.timestamp());
        assertArrayEquals(id, entry.id());
        assertEquals(2, entry.dataCount());
        assertArrayEquals(new int[]{data.getFirst().length, data.get(1).length}, entry.dataSizes());
        assertArrayEquals(data.getFirst(), entry.data()[0]);
        assertArrayEquals(data.get(1), entry.data()[1]);
    }

    @Test
    void testFromData() {
        var data = List.of("value1".getBytes(), "value2".getBytes());
        var entry = DataEntry.fromData(data);

        assertNotNull(entry.id());
        assertEquals(2, entry.dataCount());
        assertArrayEquals(new int[]{6, 6}, entry.dataSizes());
        assertArrayEquals("value1".getBytes(), entry.data()[0]);
        assertArrayEquals("value2".getBytes(), entry.data()[1]);
    }

    @Test
    void testFromDataWithId() {
        var data = List.of("value1".getBytes(), "value2".getBytes());
        var id = "id1".getBytes();
        var entry = DataEntry.fromData(data, id);

        assertArrayEquals(id, entry.id());
        assertEquals(2, entry.dataCount());
        assertArrayEquals(new int[]{6, 6}, entry.dataSizes());
        assertArrayEquals("value1".getBytes(), entry.data()[0]);
        assertArrayEquals("value2".getBytes(), entry.data()[1]);
    }

    @Test
    void testSize() {
        var data = List.of("value1".getBytes(), "value2".getBytes());
        var entry = DataEntry.fromData(data);

        int expectedSize = Entry.CRC_SIZE + 8 + 4 + 4 + (2 * 4) + entry.id().length + 6 + 6;
        assertEquals(expectedSize, entry.size());
    }

    @Test
    void testPopulateBuffer() {
        var data = List.of("value1".getBytes(), "value2".getBytes());
        var entry = DataEntry.fromData(data);
        var buffer = ByteBuffer.allocate(entry.size());

        entry.populateBuffer(buffer);
        buffer.flip();

        var newEntry = DataEntry.fromBuffer(buffer);
        assertEquals(entry.timestamp(), newEntry.timestamp());
        assertArrayEquals(entry.id(), newEntry.id());
        assertEquals(entry.dataCount(), newEntry.dataCount());
        assertArrayEquals(entry.dataSizes(), newEntry.dataSizes());
        assertArrayEquals(entry.data()[0], newEntry.data()[0]);
        assertArrayEquals(entry.data()[1], newEntry.data()[1]);
    }
}