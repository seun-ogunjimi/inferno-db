package com.infernodb.server.storage.io;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class KVUtilsTest {

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