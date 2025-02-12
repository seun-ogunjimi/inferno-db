package com.infernodb;

import com.infernodb.server.Server;
import com.infernodb.server.utils.ConfigProperties;
import com.infernodb.server.utils.Emoji;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

public class InfernoDB {
    private static Server server;

    public static void main(String[] args) throws IOException {

        var config = args.length > 0 ? ConfigProperties.fromArgs(args) : ConfigProperties.getInstance();
        server = new Server(UUID.randomUUID().toString(), "localhost", config.serverPort());
        server.start();
        Logger.getLogger(InfernoDB.class.getName()).info(Emoji.BANNER);
    }
}