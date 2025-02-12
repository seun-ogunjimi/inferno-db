package com.infernodb.server.handler;

import com.infernodb.server.storage.KVBucket;
import com.infernodb.server.storage.KVStore;
import com.infernodb.server.utils.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import static org.mockito.Mockito.*;

class KVStoreHttpHandlerTest {

    private KVStoreHttpHandler handler;
    private HttpExchange exchange;
    private KVStore kvStore;
    private KVBucket kvBucket;

    @BeforeEach
    void setUp() {
        kvStore = mock(KVStore.class);
        kvBucket = mock(KVBucket.class);
        handler = new KVStoreHttpHandler(kvStore);

        exchange = mock(HttpExchange.class);
        when(exchange.getRequestURI()).thenReturn(URI.create("/inferno/testBucket/keys/testKey"));
        when(exchange.getResponseBody()).thenReturn(mock(OutputStream.class));
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
    }

    @Test
    void handleGetRequest() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(kvStore.getBucket("testBucket")).thenReturn(kvBucket);
        when(kvBucket.get("testKey")).thenReturn("testValue");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        verify(exchange.getResponseBody()).write(JsonParser.toKeyValueJson("testKey", "testValue").getBytes());
    }

    @Test
    void handlePutRequest() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("PUT");
        when(kvStore.getBucket("testBucket")).thenReturn(kvBucket);
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream("{\"key\":\"testKey\",\"value\":\"testValue\"}".getBytes()));

        handler.handle(exchange);

        verify(kvBucket).put("testKey", "testValue");
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        //verify(exchange.getResponseBody()).write(eq(JsonParser.toHttpStatusJson(HttpStatus.OK).getBytes()));
    }

    @Test
    void handleDeleteRequest() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("DELETE");
        when(kvStore.getBucket("testBucket")).thenReturn(kvBucket);

        handler.handle(exchange);

        verify(kvBucket).delete("testKey");
        verify(exchange).sendResponseHeaders(eq(204), anyLong());
        //verify(exchange.getResponseBody()).write(JsonParser.toHttpStatusJson(HttpStatus.NO_CONTENT).getBytes());
    }

    @Test
    void handleInvalidMethod() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("POST");

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(405), anyLong());
    }
}