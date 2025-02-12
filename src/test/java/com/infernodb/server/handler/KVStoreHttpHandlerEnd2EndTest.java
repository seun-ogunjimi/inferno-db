package com.infernodb.server.handler;

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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KVStoreHttpHandlerEnd2EndTest {

    private HttpServer server;
    private KVStore kvStore;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("kvstore");
        Files.createDirectory(tempDir.resolve("testBucket"));
        kvStore = new KVStore(tempDir);

        server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/inferno", new KVStoreHttpHandler(kvStore));
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.stop(0);
        kvStore.close();
        Files.walk(tempDir)
                .map(Path::toFile)
                .forEach(file -> {
                    if (!file.delete()) {
                        file.deleteOnExit();
                    }
                });
    }

    @Test
    void testHandleGetRequest() throws IOException, URISyntaxException, InterruptedException {
        kvStore.getBucket("testBucket").put("testKey", "testValue");
        URI uri = new URI("http://localhost:8080/inferno/testBucket/keys/testKey");
        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .timeout(java.time.Duration.ofSeconds(5)) // Timeout
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
                .timeout(java.time.Duration.ofSeconds(5)) // Timeout
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("testValue", kvStore.getBucket("testBucket").get("testKey"));
    }

    @Test
    void testHandleDeleteRequest() throws IOException, URISyntaxException, InterruptedException {
        kvStore.getBucket("testBucket").put("testKey", "testValue");
        URI uri = new URI("http://localhost:8080/inferno/testBucket/keys/testKey");
        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .DELETE()
                .timeout(java.time.Duration.ofSeconds(5)) // Timeout
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
        assertEquals(null, kvStore.getBucket("testBucket").get("testKey"));
    }

    @Test
    void testHandleInvalidMethod() throws IOException, URISyntaxException, InterruptedException {
        URI uri = new URI("http://localhost:8080/inferno/testBucket/keys/testKey");
        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .timeout(java.time.Duration.ofSeconds(5)) // Timeout
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }
}