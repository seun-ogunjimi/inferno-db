package com.infernodb.server;

import com.infernodb.server.storage.KVStore;
import com.infernodb.utils.TestUtils;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ServerTest {

    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private Server server;
    private KVStore kvStore;
    private HttpServer httpServer;
    private MockedStatic<HttpServer> httpServerMockedStatic;
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("server_test");
        System.setProperty("user.home", tempDir.toString());

        kvStore = mock(KVStore.class);
        httpServer = mock(HttpServer.class);
        server = spy(new Server("testId", "127.0.0.1", 8080));

        //doReturn(kvStore).when(server).createKVStore(anyString());
        TestUtils.setPrivateField(server, "kvStore", kvStore);
        TestUtils.setPrivateField(server, "httpServer", httpServer);


        // Mock HttpServer.create
        httpServerMockedStatic = mockStatic(HttpServer.class);
        httpServerMockedStatic.when(() -> HttpServer.create(any(InetSocketAddress.class), anyInt())).thenReturn(httpServer);
    }

    @Test
    void start() throws IOException {
        //Given
        //When
        server.start();
        //Then
        verify(httpServer).start();
        assertNotNull(server);
    }

    @Test
    void stop() throws IOException {
        //Given
        //When
        server.stop();
        //Then
        verify(kvStore).close();
        verify(httpServer).stop(0);
        assertNotNull(server);
    }

    @AfterEach
    void tearDown() throws IOException {
        httpServerMockedStatic.close();
        Files.walk(tempDir)
                .map(Path::toFile)
                .forEach(file -> {
                    if (!file.delete()) {
                        file.deleteOnExit();
                    }
                });
    }
}