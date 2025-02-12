package com.infernodb.server.utils;

import com.infernodb.server.http.HttpStatus;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for parsing JSON objects
 * JSON object format:
 * {"key":"key1","value":"value1"}
 */
public interface JsonParser {
     Pattern KEY_VALUE_PATTERN = Pattern.compile("^\\{\\s*\"key\"\\s*:\\s*\"(?<key>[^\"]+)\"\\s*,\\s*\"value\"\\s*:\\s*\"(?<value>[^\"]+)\"\\s*\\}$");

     String KEY_PARAM = "key";
     String VALUE_PARAM = "value";

    /*
     * Converts a key-value pair to a JSON string
     *
     * @param kv Key-value pair
     * @return JSON string
     */
    static String toKeyValueJson(Map.Entry<String, String> kv) {
        return String.format("{\"key\":\"%s\",\"value\":\"%s\"}", kv.getKey(), kv.getValue());
    }

    /*
     * Converts a key-value pair to a JSON string
     */
    static String toKeyValueJson(String key, String value) {
        return String.format("{\"key\":\"%s\",\"value\":\"%s\"}", key, value);
    }

    /*
     * Converts a JSON object to a key-value pair
     *
     * @param json object string
     * @return Key-value pair
     */
    static Map.Entry<String, String> fromKeyValueJson(String json) {
        if (json == null || json.isEmpty()) {
            throw new IllegalArgumentException("Invalid JSON format");
        }
        json = json.trim();
        var matcher = KEY_VALUE_PATTERN.matcher(json.trim());
        if (matcher.matches()) {
            var key = matcher.group(KEY_PARAM);
            var value = matcher.group(VALUE_PARAM);
            return new AbstractMap.SimpleImmutableEntry<>(key, value);
        } else {
            throw new IllegalArgumentException("Invalid JSON format");
        }
    }

    /*
     * Converts a map of key-value pairs to a JSON array
     *
     * @param kvMap Map of key-value pairs
     * @return JSON array string
     */
    static String toKeyValueJsonArray(Map<String, String> kvMap) {
        var sb = new StringBuilder();
        sb.append("[");
        var size = kvMap.size();
        var i = 0;
        for (var entry : kvMap.entrySet()) {
            sb.append(toKeyValueJson(entry));
            if (i < size - 1) {
                sb.append(",");
            }
            i++;
        }
        sb.append("]");
        return sb.toString();
    }

    /*
     * Converts a JSON array to a map of key-value pairs
     * The JSON array should be in the format: [{"key":"key1","value":"value1"},{"key":"key2","value":"value2"}]
     * Returns an empty map if the JSON array is empty
     *
     * @param json array string
     * @return Map of key-value pairs
     */
    static Map<String, String> fromKeyValueJsonArray(String json) {
        json = json.trim();
        if (json.startsWith("[") && json.endsWith("]")) {
            json = json.substring(1, json.length() - 1).trim();
        }

        // Split the string by JSON object boundaries
        var jsonList = fromMultipleKeyValueJson(json);

        // Parse each JSON object
        var kvMap = new HashMap<String, String>();
        for (var jsonObject : jsonList) {
            var entry = fromKeyValueJson(jsonObject);
            kvMap.put(entry.getKey(), entry.getValue());
        }
        return kvMap;
    }

    private static ArrayList<String> fromMultipleKeyValueJson(String json) {
        var jsonList = new ArrayList<String>();
        int braceCount = 0;
        int startIndex = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (braceCount == 0) {
                    startIndex = i;
                }
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    jsonList.add(json.substring(startIndex, i + 1));
                }
            }
        }
        return jsonList;
    }

    /*
     * Converts an HTTP status to a JSON string
     *
     * @param status HTTP status
     * @return JSON string
     */
     static String toHttpStatusJson(HttpStatus status) {
        return toHttpStatusJson(status, null);
    }

    /*
     * Converts an HTTP status to a JSON string
     *
     * @param status HTTP status
     * @param message Custom message
     * @return JSON string
     */
    static String toHttpStatusJson(HttpStatus status, String message) {
        return String.format("{\"timestamp\":\"%s\",\"status\":\"%s\",\"message\":\"%s\"}",
                System.currentTimeMillis(), status.getCode(), message == null ? status.getReason() : message);
    }
}
