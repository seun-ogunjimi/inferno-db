package com.infernodb.server.storage;

import java.nio.ByteBuffer;
/**
 * Represents an entry for data storage.
 */
public interface Entry {
    int CRC_SIZE = 8;

    int size();

    void populateBuffer(ByteBuffer buffer);
}
