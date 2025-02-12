package com.infernodb.server;

import com.infernodb.core.cluster.AbstractNode;
import com.infernodb.core.utils.LogUtils;
import com.infernodb.server.handler.KVStoreHttpHandler;
import com.infernodb.server.http.HttpStatus;
import com.infernodb.server.http.HttpUtils;
import com.infernodb.server.storage.KVStore;
import com.infernodb.server.utils.Emoji;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A server that listens for incoming requests and stores key-value pairs.
 * <p>
 * The server listens for incoming requests on the specified IP address and port.
 * The server stores key-value pairs in a key-value store (KVStore).
 * The server can be started and stopped.
 * </p>
 */
public class Server extends AbstractNode<String> {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private static final String KV_STORE_NAME = ".inferno";
    private HttpServer httpServer;
    private KVStore kvStore;
    private boolean isRunning;


    /**
     * Create a new server with the specified IP address and port.
     *
     * @param id        the unique identifier of the server
     * @param ipAddress the IP address of the server
     * @param port      the port number of the server
     */
    public Server(String id, String ipAddress, int port) {
        super(id, ipAddress, port);
    }

    /**
     * Start the server and listen for incoming requests on the specified port.
     * <p>
     * The server will be started on the IP address and port specified in the constructor.
     * The server will be stopped when a shutdown request is received.
     * The server will also be stopped when the JVM is terminated.
     * </p>
     */
    public void start() throws IOException {
        LogUtils.configureLogger(Path.of(System.getProperty("user.home")));
        kvStore = createKVStore(KV_STORE_NAME);
        if (kvStore == null) {
            LOGGER.severe("Failed to create KVStore");
            System.exit(1);
        }
        httpServer = HttpServer.create(new InetSocketAddress(getPort()), 0);
        // Define routes
        httpServer.createContext(KVStoreHttpHandler.PATH, new KVStoreHttpHandler(kvStore));
        httpServer.createContext("/shutdown", exchange -> {
            HttpUtils.sendTextResponse(exchange, HttpStatus.OK, "Shutting down server... Thanks for using InfernoDB!");
            exchange.close();
            stop();
        });
        httpServer.setExecutor(null);
        httpServer.start();
        isRunning = true;

        //shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        LOGGER.info("Server started at " + getIpAddress() + ":" + getPort());
        LOGGER.info("Press Ctrl+C to stop the server.");
        LOGGER.info(Emoji.SERVER_ONLINE);
    }

    /**
     * Create a new KVStore with the specified directory name.
     *
     * @param storeDirName the name of the directory to store the KVStore
     * @return a new KVStore instance
     */
    private KVStore createKVStore(String storeDirName) {
        try {
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                var storePath = Path.of(userHome, storeDirName);
                return new KVStore(storePath);
            } else {
                LOGGER.warning("User home directory not found.");
                System.exit(1);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean isAlive() {
        return isRunning();
    }

    public boolean isRunning() {
        return httpServer == null || isRunning;
    }

    /**
     * Stop the server and close the KVStore.
     * <p>
     * This method will close the KVStore and stop the server.
     * The server will be stopped with a status code of 0.
     * The KVStore will be closed and any resources will be released.
     * The server will be stopped immediately and any ongoing requests will be interrupted.
     * The server will not accept any new requests.
     * </p>
     */
    public void stop() {
        LOGGER.info("Stopping server...");
        if (kvStore != null) {
            kvStore.close();
        }
        if (httpServer != null) {
            httpServer.stop(0);
        }
        isRunning = false;
        LOGGER.info("Server stopped.");
        LOGGER.info(Emoji.SHUT_DOWN);
    }
}

