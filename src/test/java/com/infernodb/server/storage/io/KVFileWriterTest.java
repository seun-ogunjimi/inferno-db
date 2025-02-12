package com.infernodb.server.storage.io;

import com.infernodb.server.storage.KVEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KVFileWriterTest {

    private KVFileWriter kvFileWriter;
    private Path filePath;

    @BeforeEach
    void setUp() throws IOException {
        filePath = Files.createTempFile("test", ".txt");
        kvFileWriter = new KVFileWriter(filePath);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (kvFileWriter != null) {
            kvFileWriter.close();
        }
        Files.deleteIfExists(filePath);
    }

    @Test
    void write() throws IOException {
        // Given
        var kvEntry = KVEntry.fromKeyValue("testKey".getBytes(), "testValue".getBytes());

        // When
        var keyDirEntry = kvFileWriter.write(kvEntry);

        // Then
        assertNotNull(keyDirEntry);
        assertEquals("testKey", new String(keyDirEntry.key()));
        assertTrue(Files.size(filePath) > 0);
    }

    @Test
    void writeAll() throws IOException {
        // Given
        var kvEntry1 = KVEntry.fromKeyValue("key1".getBytes(), "value1".getBytes());
        var kvEntry2 = KVEntry.fromKeyValue("key2".getBytes(), "value2".getBytes());
        var kvEntries = List.of(kvEntry1, kvEntry2);

        // When
        var keyDirEntries = kvFileWriter.writeAll(kvEntries);

        // Then
        assertNotNull(keyDirEntries);
        assertEquals(2, keyDirEntries.size());
        assertTrue(Files.size(filePath) > 0);
    }

    @Test
    void isOpen() {
        // When
        var isOpen = kvFileWriter.isOpen();

        // Then
        assertTrue(isOpen);
    }

    @Test
    void close() throws IOException {
        // When
        kvFileWriter.close();

        // Then
        assertFalse(kvFileWriter.isOpen());
    }
}