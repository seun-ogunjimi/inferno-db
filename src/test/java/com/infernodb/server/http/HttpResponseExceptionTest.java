package com.infernodb.server.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpResponseExceptionTest {

    @Test
    void testStatusConstructor() {
        var status = HttpStatus.BAD_REQUEST;
        var exception = new HttpResponseException(status);

        assertEquals(status, exception.getStatus());
        assertNull(exception.getResponseEntity());
    }

    @Test
    void testStatusAndMessageConstructor() {
        var status = HttpStatus.NOT_FOUND;
        var message = "Resource not found";
        var exception = new HttpResponseException(status, message);

        assertEquals(status, exception.getStatus());
        assertEquals(message, exception.getMessage());
        assertNull(exception.getResponseEntity());
    }

    @Test
    void testStatusMessageAndCauseConstructor() {
        var status = HttpStatus.INTERNAL_SERVER_ERROR;
        var message = "Internal error";
        var cause = new RuntimeException("Cause");
        var exception = new HttpResponseException(status, message, cause);

        assertEquals(status, exception.getStatus());
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getResponseEntity());
    }

    @Test
    void testResponseEntityConstructor() {
        var responseEntity = new ResponseEntity<>(HttpStatus.UNAUTHORIZED, "Unauthorized");
        var exception = new HttpResponseException(responseEntity);

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals(responseEntity, exception.getResponseEntity());
    }
}