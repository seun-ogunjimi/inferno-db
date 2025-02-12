package com.infernodb.server.handler;

import com.infernodb.server.storage.KVBucket;
import com.infernodb.server.storage.KVStore;
import com.infernodb.server.utils.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class KVStoreHttpHandlerIntegrationTest {

    private HttpServer server;
    private KVStore kvStore;
    private KVBucket kvBucket;

    @BeforeEach
    void setUp() throws IOException {
        kvStore = mock(KVStore.class);
        kvBucket = mock(KVBucket.class);
        when(kvStore.getBucket("testBucket")).thenReturn(kvBucket);

        server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/inferno", new KVStoreHttpHandler(kvStore));
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void testHandleGetRequest() throws IOException, URISyntaxException, InterruptedException {
        when(kvBucket.get("testKey")).thenReturn("testValue");

        URI uri = new URI("http://localhost:8080/inferno/testBucket/keys/testKey");
        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .timeout(java.time.Duration.ofSeconds(5))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(JsonParser.toKeyValueJson("testKey", "testValue"), response.body());
    }

    @Test
    void testHandlePutRequest() throws IOException, URISyntaxException, InterruptedException {
        URI uri = new URI("http://localhost:8080/inferno/testBucket/keys/testKey");
        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .PUT(HttpRequest.BodyPublishers.ofString("{\"key\":\"testKey\",\"value\":\"testValue\"}"))
                .timeout(java.time.Duration.ofSeconds(5))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        verify(kvBucket).put("testKey", "testValue");
    }

    @Test
    void testHandleDeleteRequest() throws IOException, URISyntaxException, InterruptedException {
        when(kvBucket.get("testKey")).thenReturn("testValue");

        URI uri = new URI("http://localhost:8080/inferno/testBucket/keys/testKey");
        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .DELETE()
                .timeout(java.time.Duration.ofSeconds(5))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
        verify(kvBucket).delete("testKey");
    }

    @Test
    void testHandleInvalidMethod() throws IOException, URISyntaxException, InterruptedException {
        URI uri = new URI("http://localhost:8080/inferno/testBucket/keys/testKey");
        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .timeout(java.time.Duration.ofSeconds(5))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }
}