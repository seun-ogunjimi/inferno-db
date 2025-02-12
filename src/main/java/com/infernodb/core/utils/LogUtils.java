package com.infernodb.core.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface LogUtils {

    static void configureLogger(Path dirPath) throws IOException {
        var rootLogger = Logger.getLogger(""); // Get root logger
        rootLogger.setLevel(Level.ALL); // Set root level

        for (var handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Add console handler
        var consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO); // Set logging level for console
        rootLogger.addHandler(consoleHandler);

        // Add file handler (log to a file)
       /* var fileHandler = new FileHandler("inferno.log", (1024 * 1024) * 5, 20, true); // Append to file
        fileHandler.setLevel(Level.ALL);*/
        //fileHandler.setFormatter(new SimpleFormatter());
        // rootLogger.addHandler(fileHandler);
    }
}
