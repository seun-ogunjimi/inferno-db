package com.infernodb.server.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpUtilsTest {

    private HttpExchange exchange;

    @BeforeEach
    void setUp() {
        exchange = mock(HttpExchange.class);
    }

    @Test
    void readRequestBody() throws IOException {
        var requestBody = "test body";
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(requestBody.getBytes()));
        var headers = new Headers();
        headers.put("Content-Length", List.of(String.valueOf(requestBody.length())));
        when(exchange.getRequestHeaders()).thenReturn(headers);

        var result = HttpUtils.readRequestBody(exchange);

        assertEquals(requestBody, result);
    }

    @Test
    void getContentLength() {
        var headers = new Headers();
        headers.put("Content-Length", List.of("123"));
        when(exchange.getRequestHeaders()).thenReturn(headers);

        var contentLength = HttpUtils.getContentLength(exchange);

        assertEquals(123, contentLength);
    }

    @Test
    void sendTextResponse() throws IOException {
        var responseBody = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(responseBody);
        when(exchange.getResponseHeaders()).thenReturn(new Headers());

        HttpUtils.sendTextResponse(exchange, HttpStatus.OK, "OK");

        verify(exchange).sendResponseHeaders(HttpStatus.OK.getCode(), "OK".length());
        assertEquals("OK", responseBody.toString());
    }

    @Test
    void sendJsonResponse() throws IOException {
        var responseBody = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(responseBody);
        when(exchange.getResponseHeaders()).thenReturn(new Headers());

        var jsonResponse = "{\"key\":\"value\"}";
        HttpUtils.sendJsonResponse(exchange, HttpStatus.OK, jsonResponse);

        verify(exchange).sendResponseHeaders(HttpStatus.OK.getCode(), jsonResponse.length());
        assertEquals(jsonResponse, responseBody.toString());
    }

    @Test
    void parseQueryParams() {
        var query = "key1=value1&key2=value2";

        var params = HttpUtils.parseQueryParams(query);

        assertEquals(2, params.size());
        assertEquals("value1", params.get("key1"));
        assertEquals("value2", params.get("key2"));
    }

    @Test
    void decodeUrl() {
        var encoded = "key%20with%20spaces";
        var decoded = HttpUtils.decodeUrl(encoded);

        assertEquals("key with spaces", decoded);
    }

    @Test
    void call() {
        Callable<ResponseEntity<String>> callable = () -> new ResponseEntity<>(HttpStatus.OK, "Success");
        var response = HttpUtils.call(callable, null);

        assertTrue(response.isPresent());
        assertEquals(HttpStatus.OK, response.get().getStatus());
        assertEquals("Success", response.get().getBody());
    }
}