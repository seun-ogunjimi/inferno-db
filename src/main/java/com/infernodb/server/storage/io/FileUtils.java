package com.infernodb.server.storage.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utility class for file operations.
 */
public interface FileUtils {

    /**
     * Creates a directory if it does not exist.
     *
     * @param dir
     * @return the directory path
     * @throws IOException
     */
    static Path createDirIfNotExists(Path dir) throws IOException {
        Objects.requireNonNull(dir, "Directory path must not be null");
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    /**
     * Creates a file if it does not exist.
     *
     * @param file
     * @return the file path
     * @throws IOException
     */
    static Path createFileIfNotExists(Path file) throws IOException {
        Objects.requireNonNull(file, "File path must not be null");
        if (Files.notExists(file)) {
            Files.createFile(file);
        }
        return file;
    }
}
