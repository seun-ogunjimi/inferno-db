package com.infernodb;

import com.infernodb.server.Server;
import com.infernodb.server.utils.ConfigProperties;
import com.infernodb.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class InfernoDBTest {
    private Path tempDir;
    private Server server;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("server_test");
        System.setProperty("user.home", tempDir.toString());

    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .map(Path::toFile)
                .forEach(file -> {
                    if (!file.delete()) {
                        file.deleteOnExit();
                    }
                });
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void testMainWithArgs() throws IOException {
        // Run the main method with test arguments
        String[] args = {"--server-port=9191", "--max-memory-size=2048", "--max-file-size=2048", "--compaction-threshold=4"};
        assertDoesNotThrow(() -> InfernoDB.main(args));

        server = TestUtils.getPrivateField(InfernoDB.class, "server", Server.class);
        // Verify that the server was started
        //verify(serverMock, times(1)).start();
        assertNotNull(server);
        assertTrue(server.isRunning());
        assertEquals(4, ConfigProperties.getInstance().compactionThreshold());
        assertEquals(2048, ConfigProperties.getInstance().maxFileSize());
        assertEquals(2048, ConfigProperties.getInstance().maxMemorySize());
        assertEquals(9191, ConfigProperties.getInstance().serverPort());
    }

    @Test
    void testMainWithoutArgs() throws IOException {
        // Run the main method without arguments
        assertDoesNotThrow(() -> InfernoDB.main(new String[]{}));

        server = TestUtils.getPrivateField(InfernoDB.class, "server", Server.class);
        // Verify that the server was started
        assertNotNull(server);
        assertTrue(server.isRunning());
    }

}