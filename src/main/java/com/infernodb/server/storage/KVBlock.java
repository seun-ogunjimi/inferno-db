package com.infernodb.server.storage;

import com.infernodb.server.storage.io.*;
import com.infernodb.server.utils.ConfigProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * KVBlock is a class that represents a key-value block. It contains a map of key directory entries, where each entry is a KeyDirEntry object.
 * The KVBlock class is responsible for managing the key directory entries and their operations.
 * The KVBlock class is used by the KVStore class to store and retrieve key-value pairs.
 */
public class KVBlock {
    private static final Logger LOGGER = Logger.getLogger(KVBlock.class.getName());
    private final Path path;
    private final KVFileReader reader;
    private final KVFileWriter writer;
    private final Map<String, KeyDirEntry> keyDir = new ConcurrentHashMap<>();
    private final TreeMap<Long, KeyDirEntry> sortedKeyDir = new TreeMap<>(Comparator.reverseOrder());
    private long size;

    /**
     * Constructs a new KVBlock with the specified path.
     *
     * @param path the path of the block
     * @throws IOException if an I/O error occurs
     */
    public KVBlock(Path path) throws IOException {
        this(path, ConfigProperties.getInstance().maxFileSize());
    }

    public KVBlock(Path path, long size) throws IOException {
        this.path = FileUtils.createFileIfNotExists(path);
        this.reader = new KVFileReader(path);
        this.writer = Files.isWritable(path) ? new KVFileWriter(path) : null;
        this.size = Math.max(size, 64);// 64 bytes
        init(null);
    }

    /**
     * Constructs a new KVBlock with the specified path and key directory entries.
     *
     * @param path    the path of the block
     * @param entries the key directory entries
     * @throws IOException if an I/O error occurs
     */
    public KVBlock(Path path, List<KeyDirEntry> entries) throws IOException {
        this.path = FileUtils.createFileIfNotExists(path);
        this.reader = new KVFileReader(path);
        this.writer = Files.isWritable(path) ? new KVFileWriter(path) : null;
        init(entries);
    }

    /**
     * Initializes the block with the specified key directory entries.
     *
     * @param entries the key directory entries
     * @throws IOException if an I/O error occurs
     */
    protected void init(List<KeyDirEntry> entries) throws IOException {
        var keyDirEntries = (entries == null || entries.isEmpty()) ? reader.readAllKeyDirEntry() : entries;
        for (var keyDirEntry : keyDirEntries) {
            var key = new String(keyDirEntry.key());
            keyDir.compute(key, (k, v) -> v == null || keyDirEntry.timestamp() > v.timestamp() ? keyDirEntry : v);
            sortedKeyDir.compute(keyDirEntry.timestamp(), (k, v) -> v == null || keyDirEntry.timestamp() > v.timestamp() ? keyDirEntry : v);
        }
    }

    /**
     * Gets the path of the block.
     *
     * @return the path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Gets the key directory entry with the specified key.
     *
     * @param key the key
     * @return the key directory entry
     */
    protected KeyDirEntry getKeyDirEntry(String key) {
        return keyDir.get(key);
    }

    /**
     * Gets the block name.
     *
     * @return the block name
     */
    public String getBlockName() {
        return path.getFileName().toString();
    }

    /**
     * Gets the block id.
     *
     * @return the block id
     */
    public String getBlockId() {
        return KVUtils.toBlockId(path.getFileName().toString());
    }

    /**
     * Gets the value of the key-value entry with the specified key.
     *
     * @param key the key
     * @return the value as a byte array
     * @throws IOException if an I/O error occurs
     */
    public byte[] get(String key) throws IOException {
        return get(getKeyDirEntry(key));
    }

    /**
     * Gets the value of the key-value entry with the specified key.
     *
     * @param entry the key directory entry
     * @return the value as a byte array
     * @throws IOException if an I/O error occurs
     */
    public byte[] get(KeyDirEntry entry) throws IOException {
        if (entry == null) {
            return null;
        }
        return reader.read(entry.valuePosition(), entry.valueSize());
    }

    /**
     * Puts the key-value entry to the block.
     *
     * @param key        the key
     * @param valueBytes the value as a byte array
     * @throws IOException if an I/O error occurs
     */
    public void put(String key, byte[] valueBytes) throws IOException {
        if (writer == null) {
            throw new IOException("Page is not writable");
        }
        var keyBytes = key.getBytes();
        var entry = KVEntry.fromKeyValue(keyBytes, valueBytes);
        var keyDirEntry = writer.write(entry);
        keyDir.compute(key, (k, v) -> v == null || keyDirEntry.timestamp() > v.timestamp() ? keyDirEntry : v);
    }

    /**
     * Puts the key-value entry to the block.
     *
     * @param kvEntry the key-value entry
     * @throws IOException if an I/O error occurs
     */
    public void put(KVEntry kvEntry) throws IOException {
        if (writer == null) {
            throw new IOException("Page is not writable");
        }
        var key = new String(kvEntry.key());
        var keyDirEntry = writer.write(kvEntry);
        keyDir.compute(key, (k, v) -> v == null || keyDirEntry.timestamp() > v.timestamp() ? keyDirEntry : v);
    }

    /**
     * Deletes the key-value entry with the specified key.
     *
     * @param key the key
     * @throws IOException if an I/O error occurs
     */
    public void delete(String key) throws IOException {
        put(key, new byte[0]);
    }

    /**
     * Gets the key-value entries between the specified start and end keys.
     *
     * @param startKey the start key
     * @param endKey   the end key
     * @return the key-value entries
     * @throws IOException if an I/O error occurs
     */
    public List<KVEntry> get(String startKey, String endKey) throws IOException {
        if (startKey == null && endKey == null) {
            return List.of();
        }
        var startEntry = startKey != null ? getKeyDirEntry(startKey) : null;
        var endEntry = endKey != null ? getKeyDirEntry(endKey) : null;

        return get(startEntry, endEntry);
    }

    /**
     * Gets the key-value entries between the specified start and end key directory entries.
     *
     * @param startEntry the start key directory entry
     * @param endEntry   the end key directory entry
     * @return the key-value entries
     * @throws IOException if an I/O error occurs
     */
    public List<KVEntry> get(KeyDirEntry startEntry, KeyDirEntry endEntry) throws IOException {
        if (startEntry == null && endEntry == null) {
            return List.of();
        } else if (startEntry != null && endEntry == null) {
            return reader.readAllKVEntry(startEntry.position(), -1);
        } else if (endEntry != null && startEntry == null) {
            return reader.readAllKVEntry(0, endEntry.position() + endEntry.size());
        } else if (Arrays.equals(startEntry.key(), endEntry.key())) {
            var entry = reader.readKVEntry(startEntry.position(), startEntry.size());
            return entry != null ? List.of(entry) : List.of();
        } else {
            var startLTEnd = startEntry.valuePosition() < endEntry.valuePosition();
            var start = startLTEnd ? startEntry : endEntry;
            endEntry = startLTEnd ? endEntry : startEntry;
            startEntry = start;
            return reader.readAllKVEntry(startEntry.position(), endEntry.position() + endEntry.size());
        }
    }

    /**
     * Writes the specified key-value entries to the block.
     *
     * @param kvEntries the key-value entries
     * @throws IOException if an I/O error occurs
     */
    public void put(List<KVEntry> kvEntries) throws IOException {
        var keyDirEntries = writer.writeAll(kvEntries);
        for (var keyDirEntry : keyDirEntries) {
            var key = new String(keyDirEntry.key());
            keyDir.compute(key, (k, v) -> v == null || keyDirEntry.timestamp() > v.timestamp() ? keyDirEntry : v);
        }
    }

    /**
     * Checks if the block is active.
     *
     * @return {@code true} if the block is active, {@code false} otherwise
     */
    public boolean isActive() {
        return writer != null && writer.isOpen();
    }

    /**
     * Sets the block to inactive.
     * <p>
     * The block is closed and set to read-only.
     * The block is no longer writable.
     * The block is considered full.
     * The block is available for compaction.
     * </p>
     *
     * @throws IOException if an I/O error occurs
     */
    public void setInactive() throws IOException {
        if (writer != null) {
            writer.isOpen();
            writer.close();
            if (!path.toFile().setReadOnly()) {
                LOGGER.warning("Failed to set block to read-only");
            }
        }
    }

    /**
     * Checks if the block is full.
     *
     * @return {@code true} if the block is full, {@code false} otherwise
     * @throws IOException if an I/O error occurs
     */
    public boolean isFull() throws IOException {
        return Files.size(path) >= size;
    }

    /**
     * Checks if the block is a compact block.
     *
     * @return {@code true} if the block is compact block type, {@code false} otherwise
     */
    public boolean isCompact() {
        return path.getFileName().toString().endsWith(FileExtension.COMPACT.getExtension());
    }

    /**
     * Closes the block.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
        reader.close();
    }
}
