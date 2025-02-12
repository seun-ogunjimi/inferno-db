package com.infernodb.server.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpStatusTest {

    @Test
    void getCode() {
        assertEquals(200, HttpStatus.OK.getCode());
        assertEquals(404, HttpStatus.NOT_FOUND.getCode());
    }

    @Test
    void getReason() {
        assertEquals("OK", HttpStatus.OK.getReason());
        assertEquals("Not Found", HttpStatus.NOT_FOUND.getReason());
    }

    @Test
    void is2xxSuccessful() {
        assertTrue(HttpStatus.OK.is2xxSuccessful());
        assertFalse(HttpStatus.NOT_FOUND.is2xxSuccessful());
    }

    @Test
    void values() {
        HttpStatus[] statuses = HttpStatus.values();
        assertEquals(28, statuses.length);
    }

    @Test
    void valueOf() {
        assertEquals(HttpStatus.OK, HttpStatus.valueOf("OK"));
        assertEquals(HttpStatus.NOT_FOUND, HttpStatus.valueOf("NOT_FOUND"));
    }
}