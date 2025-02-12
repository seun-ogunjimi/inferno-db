package com.infernodb.server.storage.io;

import com.infernodb.server.storage.KVEntry;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KVUtilsTest {

    @Test
    void testToKeyValueMap() {
        Set<KVEntry> kvEntries = new LinkedHashSet<>();
        kvEntries.add(KVEntry.fromKeyValue("key1".getBytes(), "value1".getBytes()));
        kvEntries.add(KVEntry.fromKeyValue("key2".getBytes(), "value2".getBytes()));
        kvEntries.add(KVEntry.fromKeyValue("key1".getBytes(), "value3".getBytes())); // Newer value for key1

        var keyValueMap = KVUtils.toKeyValueMap(kvEntries);

        assertEquals(2, keyValueMap.size());
        assertEquals("value3", keyValueMap.get("key1"));
        assertEquals("value2", keyValueMap.get("key2"));
    }

    @Test
    void testToBlockId() {
        assertEquals("block", KVUtils.toBlockId("block.kv"));
        assertEquals("block", KVUtils.toBlockId("block"));
        assertEquals("block.name", KVUtils.toBlockId("block.name.kv"));
    }

    @Test
    void testToBlockTimestamp() {
        assertEquals(1627846261L, KVUtils.toBlockTimestamp("1627846261_block"));
        assertTrue(KVUtils.toBlockTimestamp("invalid_block") > 0);
    }

    @Test
    void testCreateId() {
        var id = KVUtils.createId();
        assertNotNull(id);
        assertTrue(id.matches("\\d+_[\\w-]+"));
    }
}