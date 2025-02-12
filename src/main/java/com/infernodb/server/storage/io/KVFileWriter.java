package com.infernodb.server.storage.io;

import com.infernodb.server.storage.DataEntry;
import com.infernodb.server.storage.Entry;
import com.infernodb.server.storage.KVEntry;
import com.infernodb.server.storage.KeyDirEntry;
import com.infernodb.server.utils.ConfigProperties;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.CRC32;

public class KVFileWriter implements AutoCloseable {

    private final FileChannel fileChannel;
    private final ByteBuffer buffer;
    private final int bufferSize;
    private final CRC32 crc32 = new CRC32();
    private final Path path;
    private final String fileId;
    private final ReentrantLock wLock = new ReentrantLock(true);

    public KVFileWriter(Path path) throws IOException {
        this.path = path;
        this.fileId = path.getFileName().toString().split("\\.")[0];
        this.fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        this.bufferSize = Math.clamp(ConfigProperties.getInstance().maxMemorySize(), 1024, (1024 * 1024) * 10);
        this.buffer = ByteBuffer.allocateDirect(bufferSize);

    }

    public KeyDirEntry write(KVEntry entry) throws IOException {
        int length = entry.size();
        return writeLock(length, -1, () -> write(entry, -1));
    }

    public List<KeyDirEntry> writeAll(Collection<KVEntry> kvEntries) throws IOException {
        int length = kvEntries.stream().mapToInt(KVEntry::size).sum();
        return writeLock(length, -1, () -> writeAll(kvEntries, -1));
    }

    public void write(DataEntry entry) throws IOException {
        writeLock(entry.size(), -1, () -> {
            write(entry, -1);
            return null;
        });
    }

    private <T> T writeLock(int length, long position, Callable<T> task) throws IOException {
        try {
            wLock.lock();
            long currentPosition = position < 0 ? fileChannel.size() : position;
            try (var fileLock = fileChannel.lock(currentPosition, length, false)) {
                return task.call();
            } catch (Exception e) {
                throw new IOException(e);
            }
        } finally {
            wLock.unlock();
        }
    }

    private KeyDirEntry write(KVEntry kvEntry, long position) throws IOException {
        write((Entry) kvEntry, position);
        return KeyDirEntry.fromKVEntry(kvEntry, fileId, fileChannel.position() - kvEntry.valueSize());
    }

    private List<KeyDirEntry> writeAll(Collection<KVEntry> kvEntries, long position) throws IOException {
        Objects.requireNonNull(kvEntries, "KVEntry collection cannot be null");
        int totalLength = kvEntries.stream().mapToInt(KVEntry::size).sum();
        ByteBuffer buffer = totalLength <= this.buffer.capacity() ? this.buffer : ByteBuffer.allocateDirect(Math.min(totalLength, bufferSize));

        List<KeyDirEntry> keyDirEntries = new ArrayList<>();
        long initialPosition = position < 0 ? fileChannel.size() : position;
        long offset = initialPosition;

        buffer.clear();
        for (KVEntry kvEntry : kvEntries) {
            populateBuffer(kvEntry, buffer);
            int entrySize = kvEntry.size();
            offset += entrySize;

            KeyDirEntry keyDirEntry = KeyDirEntry.fromKVEntry(kvEntry, fileId, offset - kvEntry.valueSize());
            keyDirEntries.add(keyDirEntry);

            if (buffer.remaining() < entrySize || offset == initialPosition + totalLength) {
                buffer.flip();
                if (position < 0) {
                    fileChannel.write(buffer);
                } else {
                    fileChannel.write(buffer, keyDirEntry.position() + entrySize);
                }
                buffer.clear();
            }
        }

        fileChannel.force(true);
        return keyDirEntries;
    }

    private void write(Entry entry, long position) throws IOException {
        Objects.requireNonNull(entry, "Entry cannot be null");
        int length = entry.size();
        ByteBuffer buffer = length <= this.buffer.capacity() ? this.buffer : ByteBuffer.allocateDirect(Math.min(length, bufferSize));
        buffer.clear();
        populateBuffer(entry, buffer);
        buffer.flip();
        if (position < 0) {
            fileChannel.write(buffer);
        } else {
            fileChannel.write(buffer, position);
        }
        fileChannel.force(true);
    }

    private void populateBuffer(Entry entry, ByteBuffer buffer) {
        int startPosition = buffer.position();
        buffer.position(Entry.CRC_SIZE + startPosition);
        buffer.mark();
        entry.populateBuffer(buffer);
        buffer.limit(buffer.position());
        buffer.reset();
        long crc = computeCRC(buffer);
        buffer.position(startPosition);
        buffer.putLong(crc);
        buffer.position(buffer.limit());
        buffer.limit(buffer.capacity());
    }

    private long computeCRC(ByteBuffer buffer) {
        crc32.reset();
        crc32.update(buffer);
        return crc32.getValue();
    }

    public boolean isOpen() {
        return fileChannel != null && fileChannel.isOpen();
    }

    @Override
    public void close() throws IOException {
        if (fileChannel != null) {
            try {
                wLock.lock();
                fileChannel.close();
            } finally {
                wLock.unlock();
            }
        }
    }
}