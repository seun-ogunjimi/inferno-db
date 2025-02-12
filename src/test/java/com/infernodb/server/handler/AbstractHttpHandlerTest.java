package com.infernodb.server.handler;

import com.infernodb.server.http.HttpStatus;
import com.infernodb.server.http.ResponseEntity;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbstractHttpHandlerTest {

    private AbstractHttpHandler handler;
    private HttpExchange exchange;

    @BeforeEach
    void setUp() {
        handler = new AbstractHttpHandler(Pattern.compile("/test")) {
            @Override
            public ResponseEntity<String> handleGet(HttpExchange exchange, Map<String, String> pathParams) {
                return new ResponseEntity<>(HttpStatus.OK, "GET response");
            }

            @Override
            public ResponseEntity<String> handlePut(HttpExchange exchange, Map<String, String> pathParams) {
                return new ResponseEntity<>(HttpStatus.OK, "PUT response");
            }

            @Override
            public ResponseEntity<String> handleDelete(HttpExchange exchange, Map<String, String> pathParams) {
                return new ResponseEntity<>(HttpStatus.OK, "DELETE response");
            }
        };

        exchange = mock(HttpExchange.class);
        when(exchange.getRequestURI()).thenReturn(URI.create("/test"));
        when(exchange.getResponseBody()).thenReturn(mock(OutputStream.class));
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
    }

    @Test
    void handleGetRequest() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        verify(exchange.getResponseBody()).write("GET response".getBytes());
    }

    @Test
    void handlePutRequest() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("PUT");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        verify(exchange.getResponseBody()).write("PUT response".getBytes());
    }

    @Test
    void handleDeleteRequest() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("DELETE");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        verify(exchange.getResponseBody()).write("DELETE response".getBytes());
    }

    @Test
    void handleInvalidMethod() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(405), anyLong());
    }
}