package com.infernodb.server.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseEntityTest {

    @Test
    void testStatusOnlyConstructor() {
        var responseEntity = new ResponseEntity<>(HttpStatus.OK);
        assertEquals(HttpStatus.OK, responseEntity.getStatus());
        assertNull(responseEntity.getBody());
    }

    @Test
    void testStatusAndBodyConstructor() {
        var body = "Response Body";
        var responseEntity = new ResponseEntity<>(HttpStatus.CREATED, body);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatus());
        assertEquals(body, responseEntity.getBody());
    }

    @Test
    void testGetStatus() {
        var responseEntity = new ResponseEntity<>(HttpStatus.ACCEPTED);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatus());
    }

    @Test
    void testGetBody() {
        var body = "Test Body";
        var responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT, body);
        assertEquals(body, responseEntity.getBody());
    }
}