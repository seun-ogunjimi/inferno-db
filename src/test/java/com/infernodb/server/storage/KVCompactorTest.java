package com.infernodb.server.storage;


import com.infernodb.server.storage.io.KVFileReader;
import com.infernodb.server.storage.io.KVFileWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class KVCompactorTest {

    private KVCompactor kvCompactor;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("kvcompactor_test");
        kvCompactor = new KVCompactor(tempDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .map(Path::toFile)
                .forEach(file -> {
                    if (!file.delete()) {
                        file.deleteOnExit();
                    }
                });
    }

    @Test
    void compact() throws IOException {
        var kvEntry1 = KVEntry.fromKeyValue("key1".getBytes(), "value1".getBytes());
        var kvEntry2 = KVEntry.fromKeyValue("key2".getBytes(), "value2".getBytes());

        var block1 = createKVBlock("block1", List.of(kvEntry1));
        var block2 = createKVBlock("block2", List.of(kvEntry2));

        var compactedBlock = kvCompactor.compact(List.of(block1, block2));

        assertNotNull(compactedBlock);
        assertTrue(Files.exists(compactedBlock.getPath()));

        try (var reader = new KVFileReader(compactedBlock.getPath())) {
            var kvEntries = reader.readAllKVEntry(0, -1);
            assertEquals(2, kvEntries.size());
            assertEquals("key1", new String(kvEntries.get(0).key()));
            assertEquals("value1", new String(kvEntries.get(0).value()));
            assertEquals("key2", new String(kvEntries.get(1).key()));
            assertEquals("value2", new String(kvEntries.get(1).value()));
        }
    }

    @Test
    void readCompactBlocks() throws IOException, ExecutionException, InterruptedException {
        var kvEntry1 = KVEntry.fromKeyValue("key1".getBytes(), "value1".getBytes());
        var kvEntry2 = KVEntry.fromKeyValue("key2".getBytes(), "value2".getBytes());

        var block1 = createKVBlock("block1", List.of(kvEntry1));
        var block2 = createKVBlock("block2", List.of(kvEntry2));

        kvCompactor.compact(List.of(block1, block2));

        var executorService = Executors.newFixedThreadPool(2);
        var futureBlocks = kvCompactor.readCompactBlocks(executorService);


        assertNotNull(futureBlocks);
        assertEquals(1, futureBlocks.size());

        var futureBlock = futureBlocks.get(0);
        var entryBlock = futureBlock.get();
        assertNotNull(entryBlock);
        executorService.shutdown();

        var dataEntry = entryBlock.getKey();
        var kvBlock = entryBlock.getValue();



        assertNotNull(dataEntry);
        assertNotNull(kvBlock);
        assertTrue(Files.exists(kvBlock.getPath()));
    }

    private KVBlock createKVBlock(String blockName, List<KVEntry> kvEntries) throws IOException {
        var blockPath = tempDir.resolve(blockName + ".kv");
        try (var writer = new KVFileWriter(blockPath)) {
            var keyDirEntries = writer.writeAll(kvEntries);
            return new KVBlock(blockPath, keyDirEntries);
        }
    }
}