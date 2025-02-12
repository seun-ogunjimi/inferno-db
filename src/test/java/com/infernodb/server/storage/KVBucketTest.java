package com.infernodb.server.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KVBucketTest {

    private KVBucket kvBucket;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("kvbucket");
        kvBucket = new KVBucket(tempDir, 1024);
    }

    @AfterEach
    void tearDown() throws IOException {
        kvBucket.close();
        Files.walk(tempDir)
                .map(Path::toFile)
                .forEach(file -> {
                    if (!file.delete()) {
                        file.deleteOnExit();
                    }
                });
    }

    @Test
    void testPutAndGet() throws IOException {
        int count = 10;
        for (int i = 0; i < count; i++) {
            kvBucket.put("key" + i, "value" + i);
        }

        for (int i = 0; i < count; i++) {
            assertEquals("value" + i, kvBucket.get("key" + i));
        }
    }

    @Test
    void testDelete() throws IOException {
        kvBucket.put("key1", "value1");
        kvBucket.delete("key1");

        assertNull(kvBucket.get("key1"));
    }

    @Test
    void testGetRange() throws IOException {
        kvBucket.put("key1", "value1");
        kvBucket.put("key2", "value2");
        kvBucket.put("key3", "value3");

        Map<String, String> range = kvBucket.get("key1", "key3");

        assertEquals(3, range.size());
        assertEquals("value1", range.get("key1"));
        assertEquals("value2", range.get("key2"));
        assertEquals("value3", range.get("key3"));
    }

    @Test
    void testPutMultiple() throws IOException {
        Map<String, String> keyValueMap = Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3"
        );

        kvBucket.put(keyValueMap);

        assertEquals("value1", kvBucket.get("key1"));
        assertEquals("value2", kvBucket.get("key2"));
        assertEquals("value3", kvBucket.get("key3"));
    }

   /* @Test
    void testSanitizeBlock() throws IOException {
        // Create a mock KVBlock
        KVBlock mockBlock = mock(KVBlock.class);
        when(mockBlock.isFull()).thenReturn(true);

        // Set the active block to the mock block
        kvBucket.seActive(mockBlock);

        // Call sanitizeBlock
        kvBucket.sanitizeBlock(mockBlock);

        // Verify that a new block is created and the old block is marked as inactive
        verify(mockBlock, times(1)).setInactive();
        assertNotNull(kvBucket.getActiveKVBlock());
        assertNotEquals(mockBlock, kvBucket.getActiveKVBlock());
    }*/

    @Test
    void testRolloverBlock() throws IOException {
        // Verify the current block is marked as active
        KVBlock activeBlock = kvBucket.getActiveKVBlock();
        assertNotNull(activeBlock);
        assertTrue(activeBlock.isActive());

        // Fill the active block to trigger sanitization
        for (int i = 0; i < 50; i++) {
            kvBucket.put("key" + i, "value" + i);
        }

        // Trigger rollover
        // Verify that a new block is created and the old block is marked as inactive after rollover
        KVBlock activeBlock2 = kvBucket.getActiveKVBlock();
        assertNotNull(activeBlock2);
        assertTrue(activeBlock2.isActive());
        assertNotEquals(activeBlock, activeBlock2);

        // Verify that the old block is inactive and a new block is active
        assertNotEquals(activeBlock, activeBlock2);
        assertFalse(activeBlock.isActive());
        assertNotEquals(activeBlock, kvBucket.getActiveKVBlock());
    }

    @Test
    void testCompactionBlock() throws IOException {
        // Verify the current block is marked as active
        KVBlock activeBlock = kvBucket.getActiveKVBlock();
        assertNotNull(activeBlock);
        assertTrue(activeBlock.isActive());

        // Fill the active block to trigger sanitization
        for (int i = 0; i < 100; i++) {
            kvBucket.put("key" + i, "value" + i);
        }

        // Trigger rollover and compaction
        // Verify that a new block is created and the old block is marked as inactive after rollover
        KVBlock activeBlock2 = kvBucket.getActiveKVBlock();
        assertNotNull(activeBlock2);
        assertTrue(activeBlock2.isActive());
        assertNotEquals(activeBlock, activeBlock2);

        // Verify that the old block is inactive and a new block is active
        assertNotEquals(activeBlock, activeBlock2);
        assertFalse(activeBlock.isActive());
        assertNotEquals(activeBlock, kvBucket.getActiveKVBlock());

        //Verify that the old block is disposed and a new inactive(readonly) compacted block used for data retrieval
        var compactBlock = kvBucket.getKVBlock("key1");
        assertNotNull(compactBlock);
        assertFalse(compactBlock.isActive());
        assertNotEquals(compactBlock, activeBlock);
    }
}