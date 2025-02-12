package com.infernodb.server.storage.io;

import com.infernodb.server.storage.DataEntry;
import com.infernodb.server.storage.KVEntry;
import com.infernodb.server.storage.KeyDirEntry;
import com.infernodb.server.utils.ConfigProperties;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;

public class KVFileReader implements AutoCloseable {

    private final int bufferSize;
    private final AsynchronousFileChannel fileChannel;
    private final String fileId;

    public KVFileReader(Path path) throws IOException {
        this.fileId = path.getFileName().toString().split("\\.")[0];
        this.fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
        this.bufferSize = Math.clamp(ConfigProperties.getInstance().maxMemorySize(), 1024, (1024 * 1024) * 10);
    }

    /**
     * Reads data from the file at the given position.
     *
     * @param position the position in the file
     * @param length   the number of bytes to read
     * @return the data read from the file
     * @throws IOException if an I/O error occurs
     */
    public byte[] read(long position, int length) throws IOException {
        var minBufferLength = Math.min(length, this.bufferSize);
        return read(position, length, minBufferLength);
    }

    /**
     * Reads data from the file at the given position.
     *
     * @param position   the position in the file
     * @param length     the number of bytes to read
     * @param bufferSize the buffer size
     * @return the data read from the file
     * @throws IOException if an I/O error occurs
     */
    public byte[] read(long position, int length, int bufferSize) throws IOException {
        var data = new byte[length];
        int offset = 0;
        var buffer = ByteBuffer.allocateDirect(bufferSize);

        while (offset < length) {
            var bytesToRead = Math.min(length - offset, buffer.capacity());
            buffer.clear();
            buffer.limit(bytesToRead);
            Future<Integer> future = fileChannel.read(buffer, position + offset);
            int bytesRead;
            try {
                bytesRead = future.get();
            } catch (Exception e) {
                throw new IOException("Error reading file", e);
            }
            if (bytesRead == -1) {
                break;
            }
            buffer.flip();
            var bytesCopied = Math.min(bytesRead, length - offset);
            buffer.get(data, offset, bytesCopied);
            offset += bytesCopied;
        }
        return offset < length ? Arrays.copyOf(data, offset) : data;
    }

    /**
     * Reads all data from the file.
     *
     * @return the data read from the file
     * @throws IOException if an I/O error occurs
     */
    public byte[] readAll() throws IOException {
        if (this.fileChannel.size() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("File is too large to read all entries at once");
        }
        var length = (int) this.fileChannel.size();
        var minBufferSize = Math.min(length, this.bufferSize * 2);
        return read(0, length, minBufferSize);
    }

    /**
     * Reads a KVEntry from the file at the given position.
     *
     * @param position the position in the file
     * @param length   the number of bytes to read
     * @return the KVEntry read from the file
     * @throws IOException if an I/O error occurs
     */
    public KVEntry readKVEntry(long position, int length) throws IOException {
        var buffer = ByteBuffer.wrap(read(position, length));
        buffer.getLong(); // Skip CRC
        return KVEntry.fromBuffer(buffer);
    }

    /**
     * Reads all KVEntries from the file.
     *
     * @param startPosition the start position
     *                      (inclusive, 0-based index)
     * @param endPosition   the end position
     *                      (exclusive, 0-based index)
     *                      or -1 to read until the end of the file
     * @return the KVEntries
     * @throws IOException if an I/O error occurs
     */
    public List<KVEntry> readAllKVEntry(long startPosition, long endPosition) throws IOException {
        if (endPosition == -1) {
            endPosition = this.fileChannel.size();
        }
        if (startPosition < 0 || startPosition >= endPosition) {
            throw new IllegalArgumentException("Invalid start position = " + startPosition);
        }
        long length = endPosition - startPosition;
        if (length > Integer.MAX_VALUE || length > this.fileChannel.size()) {
            throw new IllegalArgumentException("Range is too large to read all entries at once");
        }
        var data = read(startPosition, (int) length);
        return readAllKVEntry(data, this::kvEntryMapper);
    }

    /**
     * Reads all KeyDirEntries from the file.
     *
     * @return the KeyDirEntries
     * @throws IOException if an I/O error occurs
     */
    public List<KeyDirEntry> readAllKeyDirEntry() throws IOException {
        if (this.fileChannel.size() == 0) {
            return List.of();
        }
        if (this.fileChannel.size() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("File is too large to read all entries at once");
        }
        var entries = new ArrayList<KeyDirEntry>();
        var buffer = ByteBuffer.wrap(readAll());
        while (buffer.hasRemaining()) {
            buffer.getLong(); // Skip CRC
            var entry = KeyDirEntry.fromBuffer(buffer, fileId);
            entries.add(entry);
        }
        return entries;
    }

    /**
     * Reads all DataEntries from the file.
     *
     * @return the DataEntries
     * @throws IOException if an I/O error occurs
     */
    public List<DataEntry> readAllDataEntry() throws IOException {
        if (this.fileChannel.size() == 0) {
            return List.of();
        }
        if (this.fileChannel.size() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("File is too large to read all entries at once");
        }
        var entries = new ArrayList<DataEntry>();
        var buffer = ByteBuffer.wrap(readAll());
        while (buffer.hasRemaining()) {
            buffer.getLong(); // Skip CRC
            var entry = DataEntry.fromBuffer(buffer);
            entries.add(entry);
        }
        return entries;
    }

    /**
     * Reads all KVEntries from the given data.
     *
     * @param data the data
     * @return the KVEntries
     */
    private <R> List<R> readAllKVEntry(byte[] data, Function<ByteBuffer, R> entryMapper) {
        var entries = new ArrayList<R>();
        var buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
            var entry = entryMapper.apply(buffer);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /**
     * Maps a ByteBuffer to a KVEntry.
     *
     * @param buffer the ByteBuffer
     * @return the KVEntry
     */
    private KVEntry kvEntryMapper(ByteBuffer buffer) {
        buffer.getLong(); // Skip CRC
        return KVEntry.fromBuffer(buffer);
    }

    /**
     * Checks if the file channel is open.
     *
     * @return true if the file channel is open, false otherwise
     */
    public boolean isOpen() {
        return this.fileChannel.isOpen();
    }

    /**
     * Closes the file channel.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        this.fileChannel.close();
    }

}