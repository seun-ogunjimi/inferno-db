package com.infernodb.server.storage;

import com.infernodb.server.storage.io.FileExtension;
import com.infernodb.server.storage.io.FileUtils;
import com.infernodb.server.storage.io.KVUtils;
import com.infernodb.server.utils.ConfigProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a key-value bucket.
 * A bucket is a collection of key-value blocks.
 * The KVBucket class is responsible for managing the blocks and their operations.
 * The KVBucket class is used by the KVStore class to store and retrieve key-value pairs.
 * The KVBucket class also contains a compactor that compacts multiple blocks into a single block.
 * The compactor is used to reduce the number of blocks in the key-value store.
 */
public class KVBucket {
    private static final Logger LOGGER = Logger.getLogger(KVBucket.class.getName());
    private final Path path;
    private final Set<String> compactedBlocks = new HashSet<>(); //for faster search
    private final SortedMap<Long, KVBlock> blockMap = Collections.synchronizedSortedMap(new TreeMap<Long, KVBlock>(Comparator.reverseOrder()));
    private final ExecutorService executorService;
    private final Object wLock = new Object();
    private final KVCompactor compactor;
    private final int compactionThreshold;
    private final  long blockSize;

    public KVBucket(Path path) throws IOException {
        this(path, ConfigProperties.getInstance().maxFileSize()); //1MB block size and 2 blocks for compaction
    }

    public KVBucket(Path path, long blockSize) throws IOException {
        this(path, blockSize, ConfigProperties.getInstance().compactionThreshold());//2 blocks for compaction
    }

    public KVBucket(Path path, long blockSize, int compactionThreshold) throws IOException {
        this.path = FileUtils.createDirIfNotExists(path);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor(); //Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.compactor = new KVCompactor(path);
        this.blockSize = Math.clamp(blockSize, 1024L, 1024L * 1024L * 1024L); //1KB to 1GB
        this.compactionThreshold = Math.max(compactionThreshold, 2);
        init();
    }

    /**
     * Initializes the KVBucket by reading the blocks from the directory path.
     *
     * @throws IOException if an I/O error occurs
     */
    private void init() throws IOException {
        try {
            //first read compacted blocks
            readCompactBlocks(executorService);
            //then read normal blocks so that compacted normal blocks are not read
            readNormalBlocks(executorService);
        } finally {
            if (blockMap.isEmpty()) {
                createKVBlock();
            }
            //TODO: free memory of compacted blocks
        }
    }

    /**
     * Reads the blocks from the directory path.
     *
     * @param executorService the executor service
     * @return a list of future blocks
     * @throws IOException if an I/O error occurs
     */
    private List<Future<KVBlock>> readBlocks(ExecutorService executorService) throws IOException {
        try (var directoryStream = Files.newDirectoryStream(path)) {
            var blocks = new ArrayList<Future<KVBlock>>();
            directoryStream.forEach(pathFile -> {
                var filename = pathFile.getFileName().toString();
                var compacted = compactedBlocks.contains(KVUtils.toBlockId(filename));
                if (compacted) {
                    LOGGER.log(Level.INFO, "Skipping compacted block: {0}", filename);
                }
                if (!compacted && Files.isRegularFile(pathFile)
                    && filename.endsWith(FileExtension.DATA.getExtension())) {
                    blocks.add(executorService.submit(() -> {
                        try {
                            return new KVBlock(pathFile, blockSize);
                        } catch (IOException e) {
                            LOGGER.severe("Error reading page: " + pathFile.getFileName());
                            return null;
                        }
                    }));
                }
            });
            return blocks;
        }
    }

    /**
     * Reads the normal blocks from the directory path.
     *
     * @param executorService the executor service
     * @throws IOException if an I/O error occurs
     */
    private void readNormalBlocks(ExecutorService executorService) throws IOException {
        readBlocks(executorService).forEach(entryFuture -> {
            try {
                var kvBlock = entryFuture.get();
                if (kvBlock != null) {
                    var id = kvBlock.getBlockId();
                    var timestamp = KVUtils.toBlockTimestamp(id);
                    blockMap.put(timestamp, kvBlock);
                }
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.severe("Error reading page: " + e.getMessage());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                } else {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    /**
     * Reads the compacted blocks from the directory path.
     *
     * @param executorService the executor service
     * @throws IOException if an I/O error occurs
     */
    private void readCompactBlocks(ExecutorService executorService) throws IOException {
        compactor.readCompactBlocks(executorService).forEach(entryFuture -> {
            try {
                var dataBlock = entryFuture.get();
                if (dataBlock != null) {
                    var dataEntry = dataBlock.getKey();
                    var kvBlock = dataBlock.getValue();
                    var dataBlockIds = Arrays.stream(dataEntry.data()).map(String::new).toList();
                    blockMap.put(dataEntry.timestamp(), kvBlock);
                    compactedBlocks.addAll(dataBlockIds);
                }
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.severe("Error reading compact file: " + e.getMessage());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                } else {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    /**
     * Creates a new key-value block.
     *
     * @return the created key-value block
     * @throws IOException if an I/O error occurs
     */
    private KVBlock createKVBlock() throws IOException {
        var timestamp = System.currentTimeMillis();
        var bucketId = timestamp + "_" + UUID.randomUUID();
        var kvBlock = new KVBlock(path.resolve(bucketId + FileExtension.DATA.getExtension()), blockSize);
        blockMap.put(timestamp, kvBlock);
        return kvBlock;
    }

    /**
     * Gets the segmented file from the path.
     *
     * @param path the path
     * @return the segmented file
     * @throws IOException if an I/O error occurs
     */
    private Path getSegmentedFile(Path path) throws IOException {
        var fileName = path.getFileName().toString();
        var compactedPath = path.getParent().resolve(fileName + FileExtension.COMPACT.getExtension());
        return Files.exists(compactedPath) && Files.size(compactedPath) > 0
               && Files.isRegularFile(compactedPath) && !Files.isWritable(compactedPath) ? compactedPath : path;
    }

    /**
     * Gets the key directory entry for the specified key.
     *
     * @param key the key
     * @return the key directory entry
     */
    private Map.Entry<KeyDirEntry, KVBlock> getKeyDirEntry(String key) {
        for (Map.Entry<Long, KVBlock> entry : blockMap.entrySet()) {
            var kvBlock = entry.getValue();
            var keyDirEntry = kvBlock.getKeyDirEntry(key);
            if (keyDirEntry != null) {
                return Map.entry(keyDirEntry, kvBlock);
            }
        }
        return null;
    }

    /**
     * Gets the active key-value block.
     *
     * @return the active key-value block
     */
    public KVBlock getActiveKVBlock() {
        for (Map.Entry<Long, KVBlock> entry : blockMap.entrySet()) {
            var kvBlock = entry.getValue();
            if (kvBlock.isActive()) {
                return kvBlock;
            }
        }
        return null;
    }

    public KVBlock getKVBlock(String key) {
        for (Map.Entry<Long, KVBlock> entry : blockMap.entrySet()) {
            var kvBlock = entry.getValue();
            if (kvBlock.getKeyDirEntry(key) != null) {
                return kvBlock;
            }
        }
        return null;
    }

    /**
     * Gets the key-value block for the specified key.
     *
     * @param key the key
     * @return the key-value block
     */
    public String get(String key) throws IOException {
        var mapEntry = getKeyDirEntry(key);
        if (mapEntry == null) {
            return null;
        }
        var keyDirEntry = mapEntry.getKey();
        var kvBlock = mapEntry.getValue();
        var k = kvBlock.get(keyDirEntry);
        return (k == null || k.length == 0) ? null : new String(k);
    }

    /**
     * Puts the key-value pair in the bucket.
     *
     * @param key   the key
     * @param value the value
     * @throws IOException if an I/O error occurs
     */
    public void put(String key, String value) throws IOException {
        synchronized (wLock) {
            var kvBlock = getActiveKVBlock();
            if (kvBlock == null) {
                throw new IllegalArgumentException("No active block found");
            }
            kvBlock.put(key, value.getBytes());
            sanitizeBlock(kvBlock);
        }
    }

    /**
     * Deletes the key from the bucket.
     *
     * @param key the key
     * @throws IOException if an I/O error occurs
     */
    public void delete(String key) throws IOException {
        synchronized (wLock) {
            var kvBlock = getActiveKVBlock();
            if (kvBlock == null) {
                throw new IllegalArgumentException("No active block found");
            }
            kvBlock.delete(key);
            sanitizeBlock(kvBlock);
        }
    }

    /**
     * Gets the key-value pairs for the specified range of keys.
     *
     * @param startKey the start key
     * @param endKey   the end key
     * @return the key-value pairs
     * @throws IOException if an I/O error occurs
     */
    public Map<String, String> get(String startKey, String endKey) throws IOException {
        if (startKey == null && endKey == null) {
            return Collections.emptyMap();
        }
        var startKeyEntry = startKey != null ? getKeyDirEntry(startKey) : null;
        Map.Entry<KeyDirEntry, KVBlock> endKeyEntry = null;

        if (endKey != null) {
            var keyDirEntry = startKeyEntry != null ? startKeyEntry.getValue().getKeyDirEntry(endKey) : null;
            endKeyEntry = keyDirEntry != null ? Map.entry(keyDirEntry, startKeyEntry.getValue()) : getKeyDirEntry(endKey);
        }
        if (startKeyEntry == null && endKeyEntry == null) {
            return Collections.emptyMap();
        }
        var entries = getRange(startKeyEntry, endKeyEntry);
        return KVUtils.toKeyValueMap(entries);
    }

    /**
     * Gets the key-value pairs for the specified range of keys.
     *
     * @param startKeyEntry the start key entry
     * @param endKeyEntry   the end key entry
     * @return the key-value pairs
     * @throws IOException if an I/O error occurs
     */
    private Set<KVEntry> getRange(Map.Entry<KeyDirEntry, KVBlock> startKeyEntry, Map.Entry<KeyDirEntry, KVBlock> endKeyEntry) throws IOException {
        var entries = new TreeSet<KVEntry>((e1, e2) -> {
            if (e1.timestamp() == e2.timestamp()) {
                return 0;
            }
            return e1.timestamp() < e2.timestamp() ? -1 : 1;
        });
        if (startKeyEntry != null && (startKeyEntry == endKeyEntry || startKeyEntry.getValue() == endKeyEntry.getValue())) {
            var entryList = startKeyEntry.getValue().get(startKeyEntry.getKey(), endKeyEntry.getKey());
            entries.addAll(entryList);
        } else {
            // get all entries between startEntry and endEntry
            if (startKeyEntry != null) {
                var startEntries = startKeyEntry.getValue().get(startKeyEntry.getKey(), null);
                entries.addAll(startEntries);
            }
            if (endKeyEntry != null) {
                var endEntries = endKeyEntry.getValue().get(null, endKeyEntry.getKey());
                entries.addAll(endEntries);
            }
        }
        return entries;
    }

    /**
     * Puts the key-value pairs in the bucket.
     *
     * @param keyValueMap the key-value map
     * @throws IOException if an I/O error occurs
     */
    public void put(Map<String, String> keyValueMap) throws IOException {
        var kvEntries = new ArrayList<KVEntry>();
        for (var entry : keyValueMap.entrySet()) {
            var keyBytes = entry.getKey().getBytes();
            var valueBytes = entry.getValue().getBytes();
            kvEntries.add(KVEntry.fromKeyValue(keyBytes, valueBytes));
        }
        synchronized (wLock) {
            var kvBlock = getActiveKVBlock();
            if (kvBlock == null) {
                throw new IllegalArgumentException("No active block found");
            }
            kvBlock.put(kvEntries);
            sanitizeBlock(kvBlock);
        }
    }

    /**
     * Sanitizes the block by creating a new block if the active block is full.
     *
     * @param activeBlock the active block
     * @throws IOException if an I/O error occurs
     */
    public void sanitizeBlock(KVBlock activeBlock) throws IOException {
        try {
            if (activeBlock.isFull()) {
                //rollover block
                createKVBlock();
                activeBlock.setInactive();
                //compaction
                compactBlocks();
            }
        } catch (IOException e) {
            LOGGER.severe("Error creating new block: " + e.getMessage());
        }
    }

    /**
     * Compacts the blocks into a single block.
     *
     * @throws IOException if an I/O error occurs
     */
    private void compactBlocks() throws IOException {
        if (compactionThreshold <= 0) {
            return;
        }

        var compactBlocks = new ArrayList<KVBlock>(compactionThreshold);
        for (Map.Entry<Long, KVBlock> entry : blockMap.entrySet()) {
            var kvBlock = entry.getValue();
            if (!kvBlock.isActive() && !kvBlock.isCompact() && !isCompacted(kvBlock.getBlockId())) {
                compactBlocks.add(kvBlock);
                if (compactBlocks.size() == compactionThreshold) {
                    break;
                }
            }
        }
        if (compactBlocks.size() == compactionThreshold) {
            executorService.execute(() -> {
                try {
                    compact(compactBlocks);
                } catch (IOException e) {
                    LOGGER.severe("Error compacting blocks: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Compacts the blocks into a single block.
     *
     * @param compactBlocks the compact blocks
     * @throws IOException if an I/O error occurs
     */
    private void compact(List<KVBlock> compactBlocks) throws IOException {
        var compactBlock = compactor.compact(compactBlocks);
        if (compactBlock != null) {
            var id = compactBlock.getBlockId();
            var timestamp = KVUtils.toBlockTimestamp(id);
            //TODO: time of the compacted block is the time of the last block but should be less than that of the active block
            blockMap.put(timestamp, compactBlock);
            for (var kvBlock : compactBlocks) {
                var blockId = kvBlock.getBlockId();
                if (blockMap.remove(KVUtils.toBlockTimestamp(blockId)) == kvBlock) {
                    LOGGER.log(Level.INFO, "Removed block: {0}", blockId);
                    kvBlock.close();
                    compactedBlocks.add(blockId);
                }
            }
        }
    }


    private boolean isCompacted(String blockId) {
        return compactedBlocks.contains(blockId);
    }

    /**
     * Gets the path of the bucket.
     *
     * @return the path of the bucket
     */
    public String getBucketName() {
        return path.getFileName().toString();
    }

    /*public long getBlockSize() {
        return blockSize;
    }

    public int getCompactionBlockCount() {
        return compactionBlockCount;
    }
*/

    /**
     * Closes the bucket.
     */
    public void close() {
        compactor.close();
        blockMap.values().forEach(kvBlock -> {
            try {
                kvBlock.close();
            } catch (IOException e) {
                LOGGER.severe("Error closing page: " + kvBlock.getBlockName());
            }
        });

        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.severe("Error shutting down executor service: " + e.getMessage());
            Thread.currentThread().interrupt();
            executorService.shutdownNow();

        }
    }
}
