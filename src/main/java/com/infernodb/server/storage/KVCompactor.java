package com.infernodb.server.storage;

import com.infernodb.server.storage.io.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Compacts multiple blocks into a single block.
 * <p>
 * The compactor reads multiple blocks and writes a new compacted block.
 * The compacted block contains all the data entries from the blocks.
 * The compactor also writes a metadata file containing the block ids of the blocks that were compacted.
 * The metadata file is used to read the compacted blocks.
 * The compactor is used to reduce the number of blocks in the key-value store.
 * </p>
 */
public class KVCompactor implements AutoCloseable {
    public static final String COMPACT_DIR_NAME = "compacted";
    private static final Logger LOGGER = Logger.getLogger(KVCompactor.class.getName());
    private final Path path;

    public KVCompactor(Path path) {
        this.path = path;
    }

    /**
     * Compact the specified blocks into a new block.
     *
     * @param blocks the blocks to compact
     * @return a new compacted block
     * @throws IOException if an I/O error occurs
     */
    public KVBlock compact(List<KVBlock> blocks) throws IOException {
        var kvEntrySet = new TreeSet<KVEntry>(((o1, o2) -> o2.timestamp() >= o1.timestamp() ? 1 : -1));// Sorted by key
        var dataEntries = new ArrayList<byte[]>();

        for (var block : blocks) {
            try (var reader = new KVFileReader(block.getPath())) {
                // Read all data entries from the block
                var kvEntries = reader.readAllKVEntry(0, -1);
                kvEntrySet.addAll(kvEntries);
                dataEntries.add(block.getBlockId().getBytes());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e, () -> "Error reading block: " + block.getBlockName());
            }
        }

        if (!kvEntrySet.isEmpty()) {
            var id = KVUtils.createId();
            var compactedDir = FileUtils.createDirIfNotExists(path.resolve(COMPACT_DIR_NAME));
            var compactedFile = compactedDir.resolve(id + FileExtension.COMPACT.getExtension());
            try (var writer = new KVFileWriter(compactedFile)) {
                // Write all data entries to a new compacted block
                var keyDirList = writer.writeAll(kvEntrySet);

                var dataEntry = DataEntry.fromData(dataEntries, id.getBytes());
                saveMetadata(dataEntry);
                if (!compactedFile.toFile().setReadOnly()) {
                    LOGGER.log(Level.WARNING, "Failed to set compacted file to read-only");
                }
                return new KVBlock(compactedFile, keyDirList);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error writing compacted block", e);
            }
        }
        return null;
    }

    private void saveMetadata(DataEntry dataEntry) {
        try (var w = new KVFileWriter(path.resolve(path.getFileName() + FileExtension.METADATA.getExtension()))) {
            w.write(dataEntry);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error writing metadata", e);
        }
    }

    /**
     * Read all compacted blocks from the metadata file.
     *
     * @param executorService the executor service to read the compacted blocks
     * @return a list of future compacted blocks
     * @throws IOException if an I/O error occurs
     */
    public List<Future<Map.Entry<DataEntry, KVBlock>>> readCompactBlocks(ExecutorService executorService) throws IOException {
        var metadataPath = path.resolve(path.getFileName() + FileExtension.METADATA.getExtension());
        if (Files.notExists(metadataPath)) {
            LOGGER.log(Level.FINE, "Compacted metadata file not found: {0}", metadataPath);
            return Collections.emptyList();
        }
        if (Files.size(metadataPath) == 0 || !Files.isRegularFile(metadataPath)) {
            LOGGER.log(Level.SEVERE, "Invalid compacted metadata file: {0}", metadataPath);
            return Collections.emptyList();
        }
        try (var reader = new KVFileReader(metadataPath)) {
            var blocks = new ArrayList<Future<Map.Entry<DataEntry, KVBlock>>>();
            var compactedMetadata = reader.readAllDataEntry();
            for (var dataEntry : compactedMetadata) {
                var id = dataEntry.id();
                var fileName = new String(id) + FileExtension.COMPACT.getExtension();
                var compactedFile = path.resolve(COMPACT_DIR_NAME).resolve(fileName);
                if (Files.notExists(compactedFile) || Files.size(compactedFile) == 0 || !Files.isRegularFile(compactedFile)) {
                    LOGGER.log(Level.SEVERE, "Invalid compacted file: {0}", fileName);
                    continue;
                }
                blocks.add(
                        executorService.submit(() -> {
                            try {
                                return Map.entry(dataEntry, new KVBlock(compactedFile));
                            } catch (IOException e) {
                                LOGGER.log(Level.SEVERE, "Error reading compacted file: %s".formatted(e.getMessage()), e);
                                return null;
                            }
                        })
                );
            }
            return blocks;
        }
    }

    @Override
    public void close() {
        // Do nothing
    }
}