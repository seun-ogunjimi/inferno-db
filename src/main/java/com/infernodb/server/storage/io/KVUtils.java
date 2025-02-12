package com.infernodb.server.storage.io;

import com.infernodb.server.storage.KVEntry;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Utility class for key-value operations.
 */
public interface KVUtils {
    Logger LOGGER = Logger.getLogger(KVUtils.class.getName());


    /**
     * Converts a block id to a block id.
     *
     * @param blockName a block name
     * @return a block id
     */
    static String toBlockId(String blockName) {
        var ids = blockName.split("\\.");
        if (ids.length == 1) {
            return ids[0];
        }
        ids = Arrays.copyOf(ids, ids.length - 1);
        return String.join(".", ids);
    }

    /**
     * Converts a block id to a block timestamp.
     *
     * @param blockId
     * @return a block timestamp
     */
    static long toBlockTimestamp(String blockId) {
        try {
            return Long.parseLong(blockId.split("_")[0]);
        } catch (NumberFormatException e) {
            LOGGER.severe("Invalid block id: " + blockId);
        }
        return System.currentTimeMillis();
    }

    /**
     * Creates a new id.
     *
     * @return a new id
     */
    static String createId() {
        var timestamp = System.currentTimeMillis();
        var id = timestamp + "_" + UUID.randomUUID();
        return id;
    }

    static <T> T eval(Supplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }
}