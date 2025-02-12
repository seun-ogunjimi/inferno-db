package com.infernodb.server.storage;

import com.infernodb.server.storage.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * KVStore is a class that represents a key-value store. It contains a map of buckets, where each bucket is a KVBucket object.
 * The KVStore class is responsible for managing the buckets and their operations.
 * The KVStore class is used by the KVServer class to store and retrieve key-value pairs.
 */
public class KVStore {
    private static final Logger LOGGER = Logger.getLogger(KVStore.class.getName());
    private static final String DEFAULT_BUCKET = "root";
    private Path path;
    private Map<String, KVBucket> bucketMap = new HashMap<>();

    /**
     * Constructs a new KVStore with the specified directory path.
     *
     * @param dirPath the directory path where the KVStore is stored
     * @throws IOException if an I/O error occurs
     */
    public KVStore(Path dirPath) throws IOException {
        this.path = FileUtils.createDirIfNotExists(dirPath);
        init();
    }

    /**
     * Initializes the KVStore by reading the buckets from the directory path.
     *
     * @throws IOException if an I/O error occurs
     */
    private void init() throws IOException {
        try (var directoryStream = Files.newDirectoryStream(path);
             var executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            List<Future<KVBucket>> schemas = new ArrayList<>();
            directoryStream.forEach(dir -> {
                if (Files.isDirectory(dir)) {
                    schemas.add(executorService.submit(() -> new KVBucket(dir)));
                }
            });
            schemas.forEach(future -> {
                try {
                    var kvBucket = future.get();
                    bucketMap.put(kvBucket.getBucketName().toLowerCase(), kvBucket);
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.log(Level.SEVERE, "Error reading bucket: %s".formatted(e.getMessage()), e);
                    throw new RuntimeException(e);
                }
            });
        } finally {
            if (bucketMap.isEmpty()) {
                createBucket(DEFAULT_BUCKET);
            }
        }
    }

    /**
     * Create a new bucket with the given name.
     *
     * @param bucketName the name of the bucket to create
     * @return the created bucket
     * @throws IOException if an I/O error occurs
     */
    public KVBucket createBucket(String bucketName) throws IOException {
        var bucketPath = path.resolve(bucketName);
        var kvBucket = new KVBucket(bucketPath);
        bucketMap.put(kvBucket.getBucketName().toLowerCase(), kvBucket);
        return kvBucket;
    }

    /**
     * Get the bucket with the specified name.
     *
     * @param bucketName the name of the bucket to get
     * @return the bucket with the specified name
     */
    public KVBucket getBucket(String bucketName) {
        return bucketMap.get(bucketName.toLowerCase());
    }

    public void close() {
        bucketMap.values().forEach(kvBucket -> {
            try {
                kvBucket.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error closing bucket: %s".formatted(e.getMessage()), e);
            }
        });
    }
}
