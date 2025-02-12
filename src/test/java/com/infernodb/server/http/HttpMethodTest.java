package com.infernodb.server.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpMethodTest {

    @Test
    void fromString_validValue() {
        assertEquals(HttpMethod.GET, HttpMethod.fromString("GET"));
        assertEquals(HttpMethod.POST, HttpMethod.fromString("POST"));
        assertEquals(HttpMethod.PUT, HttpMethod.fromString("PUT"));
        assertEquals(HttpMethod.DELETE, HttpMethod.fromString("DELETE"));
    }

    @Test
    void fromString_invalidValue() {
        assertNull(HttpMethod.fromString("INVALID"));
    }

    @Test
    void fromString_nullValue() {
        assertNull(HttpMethod.fromString(null));
    }

    @Test
    void values() {
        var methods = HttpMethod.values();
        assertEquals(4, methods.length);
        assertArrayEquals(new HttpMethod[]{HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE}, methods);
    }

    @Test
    void valueOf() {
        assertEquals(HttpMethod.GET, HttpMethod.valueOf("GET"));
        assertEquals(HttpMethod.POST, HttpMethod.valueOf("POST"));
        assertEquals(HttpMethod.PUT, HttpMethod.valueOf("PUT"));
        assertEquals(HttpMethod.DELETE, HttpMethod.valueOf("DELETE"));
    }
}