package com.infernodb.server.storage.io;

import com.infernodb.server.storage.DataEntry;
import com.infernodb.server.storage.Entry;
import com.infernodb.server.storage.KVEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KVFileReaderTest {

    private KVFileReader kvFileReader;
    private Path filePath;

    @BeforeEach
    void setUp() throws IOException {
        filePath = Files.createTempFile("test", ".txt");
        kvFileReader = new KVFileReader(filePath);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (kvFileReader != null) {
            kvFileReader.close();
        }
        Files.deleteIfExists(filePath);
    }

    /*@Test
    void readx() throws IOException {
        var kvEntry = KVEntry.fromKeyValue("testKey".getBytes(), "testValue".getBytes());
        var kvFileWriter = new KVFileWriter(filePath);
        kvFileWriter.write(kvEntry);
        kvFileWriter.close();

        var readData = kvFileReader.readx(0, kvEntry.size() );

        assertNotNull(readData);
        assertEquals(kvEntry.size() , readData.length);
    }*/

    @Test
    void read() throws IOException {
        var kvEntry = KVEntry.fromKeyValue("testKey".getBytes(), "testValue".getBytes());
        var kvFileWriter = new KVFileWriter(filePath);
        kvFileWriter.write(kvEntry);
        kvFileWriter.close();

        var readData = kvFileReader.read(0, kvEntry.size() );

        assertNotNull(readData);
        assertEquals(kvEntry.size() , readData.length);
    }

    @Test
    void testRead() throws IOException {
        var kvEntry = KVEntry.fromKeyValue("testKey".getBytes(), "testValue".getBytes());
        var kvFileWriter = new KVFileWriter(filePath);
        kvFileWriter.write(kvEntry);
        kvFileWriter.close();

        var readData = kvFileReader.read(0, kvEntry.size() , 512);

        assertNotNull(readData);
        assertEquals(kvEntry.size() , readData.length);
    }

    @Test
    void readAll() throws IOException {
        var kvEntry = KVEntry.fromKeyValue("testKey".getBytes(), "testValue".getBytes());
        var kvFileWriter = new KVFileWriter(filePath);
        kvFileWriter.write(kvEntry);
        kvFileWriter.close();

        var readData = kvFileReader.readAll();

        assertNotNull(readData);
        assertTrue(readData.length > 0);
    }

    @Test
    void readKVEntry() throws IOException {
        var kvEntry = KVEntry.fromKeyValue("testKey".getBytes(), "testValue".getBytes());
        var kvFileWriter = new KVFileWriter(filePath);
        kvFileWriter.write(kvEntry);
        kvFileWriter.close();

        var readEntry = kvFileReader.readKVEntry(0, kvEntry.size() );

        assertNotNull(readEntry);
        assertEquals("testKey", new String(readEntry.key()));
        assertEquals("testValue", new String(readEntry.value()));
    }

    @Test
    void readAllKVEntry() throws IOException {
        var kvEntry1 = KVEntry.fromKeyValue("key1".getBytes(), "value1".getBytes());
        var kvEntry2 = KVEntry.fromKeyValue("key2".getBytes(), "value2".getBytes());
        var kvFileWriter = new KVFileWriter(filePath);
        kvFileWriter.write(kvEntry1);
        kvFileWriter.write(kvEntry2);
        kvFileWriter.close();

        var entries = kvFileReader.readAllKVEntry(0, -1);

        assertNotNull(entries);
        assertEquals(2, entries.size());
        assertEquals("key1", new String(entries.get(0).key()));
        assertEquals("value1", new String(entries.get(0).value()));
        assertEquals("key2", new String(entries.get(1).key()));
        assertEquals("value2", new String(entries.get(1).value()));
    }

    @Test
    void readAllKeyDirEntry() throws IOException {
        var kvEntry1 = KVEntry.fromKeyValue("key1".getBytes(), "value1".getBytes());
        var kvEntry2 = KVEntry.fromKeyValue("key2".getBytes(), "value2".getBytes());
        var kvFileWriter = new KVFileWriter(filePath);
        kvFileWriter.write(kvEntry1);
        kvFileWriter.write(kvEntry2);
        kvFileWriter.close();

        var keyDirEntries = kvFileReader.readAllKeyDirEntry();

        assertNotNull(keyDirEntries);
        assertEquals(2, keyDirEntries.size());
        assertEquals("key1", new String(keyDirEntries.get(0).key()));
        assertEquals("key2", new String(keyDirEntries.get(1).key()));
    }

    @Test
    void readAllDataEntry() throws IOException {
        var dataEntry1 = DataEntry.fromData(List.of("data1".getBytes()), "id1".getBytes());
        var dataEntry2 = DataEntry.fromData(List.of("data2".getBytes()), "id2".getBytes());
        var kvFileWriter = new KVFileWriter(filePath);
        kvFileWriter.write(dataEntry1);
        kvFileWriter.write(dataEntry2);
        kvFileWriter.close();

        var dataEntries = kvFileReader.readAllDataEntry();

        assertNotNull(dataEntries);
        assertEquals(2, dataEntries.size());
        assertEquals("data1", new String(dataEntries.get(0).data()[0]));
        assertEquals("id1", new String(dataEntries.get(0).id()));
        assertEquals("data2", new String(dataEntries.get(1).data()[0]));
        assertEquals("id2", new String(dataEntries.get(1).id()));
    }

    @Test
    void close() throws IOException {
        kvFileReader.close();
        assertFalse(kvFileReader.isOpen());
    }
}