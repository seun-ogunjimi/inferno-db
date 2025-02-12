package com.infernodb.server.storage.io;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

    @Test
    void createDirIfNotExists() throws IOException {
        Path dir = Paths.get("testDir");
        try {
            Path createdDir = FileUtils.createDirIfNotExists(dir);
            assertTrue(Files.exists(createdDir));
        } finally {
            Files.deleteIfExists(dir);
        }
    }

    @Test
    void createFileIfNotExists() throws IOException {
        Path file = Paths.get("testFile.txt");
        try {
            Path createdFile = FileUtils.createFileIfNotExists(file);
            assertTrue(Files.exists(createdFile));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}