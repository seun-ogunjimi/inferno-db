package com.infernodb.server.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record ConfigProperties(int serverPort, long maxMemorySize, long maxFileSize, int compactionThreshold) {
    private static ConfigProperties INSTANCE = fromSystem();

    public static ConfigProperties getInstance() {
        return INSTANCE;
    }

    static ConfigProperties fromSystem() {
        int serverPort = eval(() -> Integer.parseInt(System.getProperty("serverPort", "9090")), 9090);
        long maxFileSize = eval(() -> Long.parseLong(System.getProperty("maxFileSize", 1024L * 1024L * 10 + "")), 1024L * 1024L * 10);
        long maxMemorySize = eval(() -> Long.parseLong(System.getProperty("maxMemorySize", 1024L + "")), 1024L);
        int compactionThreshold = eval(() -> Integer.parseInt(System.getProperty("compactionThreshold", "2")), 2);
        return new ConfigProperties(serverPort, maxMemorySize, maxFileSize, compactionThreshold);
    }

    public static ConfigProperties fromArgs(String[] args) {
        var argMap = parseArgs(args);
        var config = new ConfigProperties(
                eval(() -> Integer.parseInt(argMap.get("--server-port")), 9090),
                eval(() -> Long.parseLong(argMap.get("--max-memory-size")), 1024L ),// 1KB
                eval(() -> Long.parseLong(argMap.get("--max-file-size")), 1024L * 1024L * 10), //10MB
                eval(() -> Integer.parseInt(argMap.get("--compaction-threshold")), 2));
        INSTANCE = config;
        return config;
    }

    private static <T> T eval(Supplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> argMap = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] parts = arg.split("=", 2);
                if (parts.length == 2) {

                    argMap.put(parts[0], parts[1]);
                }
            }
        }
        return argMap;
    }
}
