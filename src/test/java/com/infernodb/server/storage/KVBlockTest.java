package com.infernodb.server.storage;

import com.infernodb.server.storage.io.FileExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KVBlockTest {

    private KVBlock kvBlock;
    private Path tempFile;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("kvblock", FileExtension.DATA.getExtension());
        kvBlock = new KVBlock(tempFile);

    }

    @AfterEach
    void tearDown() throws IOException {
        kvBlock.close();
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testPutAndGet() throws IOException {
        kvBlock.put("key1", "value1".getBytes());
        kvBlock.put("key2", "value2".getBytes());

        assertArrayEquals("value1".getBytes(), kvBlock.get("key1"));
        assertArrayEquals("value2".getBytes(), kvBlock.get("key2"));
    }

    @Test
    void testDelete() throws IOException {
        kvBlock.put("key1", "value1".getBytes());
        kvBlock.delete("key1");

        // Check if the key is deleted. length of byte[] is 0
        assertEquals(kvBlock.get("key1").length,0);
    }

    @Test
    void testIsFull() throws IOException {
        kvBlock = new KVBlock(tempFile, 64); // 64 bytes

        kvBlock.put("key1", new byte[32]);
        assertFalse(kvBlock.isFull());

        kvBlock.put("key2", new byte[32]);
        assertTrue(kvBlock.isFull());
    }

    @Test
    void testSetInactive() throws IOException {
        kvBlock.put("key1", "value1".getBytes());
        kvBlock.setInactive();

        assertFalse(kvBlock.isActive());
    }

    @Test
    void testGetRange() throws IOException {
        kvBlock.put("key1", "value1".getBytes());
        kvBlock.put("key2", "value2".getBytes());
        kvBlock.put("key3", "value3".getBytes());

        var entries = kvBlock.get("key1", "key3");
        assertEquals(3, entries.size());
    }

    @Test
    void testGetPath() {
        assertEquals(tempFile, kvBlock.getPath());
    }

    @Test
    void testGetBlockName() {
        assertEquals(tempFile.getFileName().toString(), kvBlock.getBlockName());
    }

    @Test
    void testGetBlockId() {
        assertNotNull(kvBlock.getBlockId());
    }

    @Test
    void testIsCompact() {
        assertFalse(kvBlock.isCompact());
    }

    @Test
    void testClose() throws IOException {
        kvBlock.close();
        assertFalse(kvBlock.isActive());
    }
}