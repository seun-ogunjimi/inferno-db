package com.infernodb.server.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class KVStoreTest {

    private KVStore kvStore;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("kvstore_test");
    }

    @AfterEach
    void tearDown() throws IOException {
        kvStore.close();
        Files.walk(tempDir)
                .map(Path::toFile)
                .forEach(file -> {
                    if (!file.delete()) {
                        file.deleteOnExit();
                    }
                });
    }

    @Test
    void testLoadExistingBuckets() throws IOException {
        // Create pre-existing bucket directories
        String bucketName1 = "bucket1";
        String bucketName2 = "bucket2";
        Files.createDirectory(tempDir.resolve(bucketName1));
        Files.createDirectory(tempDir.resolve(bucketName2));

        // Initialize KVStore
        kvStore = new KVStore(tempDir);

        // Verify that the buckets are loaded
        assertNotNull(kvStore.getBucket(bucketName1));
        assertNotNull(kvStore.getBucket(bucketName2));
        assertEquals(bucketName1, kvStore.getBucket(bucketName1).getBucketName());
        assertEquals(bucketName2, kvStore.getBucket(bucketName2).getBucketName());
    }

    @Test
    void testCreateBucket() throws IOException {
        String bucketName = "testBucket";
        kvStore = new KVStore(tempDir);
        KVBucket bucket = kvStore.createBucket(bucketName);

        assertNotNull(bucket);
        assertEquals(bucketName, bucket.getBucketName());
        assertTrue(Files.exists(tempDir.resolve(bucketName)));
    }

    @Test
    void testGetBucket() throws IOException {
        String bucketName = "testBucket";
        kvStore = new KVStore(tempDir);
        kvStore.createBucket(bucketName);
        KVBucket bucket = kvStore.getBucket(bucketName);

        assertNotNull(bucket);
        assertEquals(bucketName, bucket.getBucketName());
    }

    @Test
    void testGetNonExistentBucket() throws IOException {
        kvStore = new KVStore(tempDir);
        KVBucket bucket = kvStore.getBucket("nonExistentBucket");

        assertNull(bucket);
    }

    @Test
    void testClose() throws IOException {
        kvStore = new KVStore(tempDir);
        assertDoesNotThrow(() -> kvStore.close());

    }
}