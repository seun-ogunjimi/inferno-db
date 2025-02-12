package com.infernodb.server.http;
/**
 * Enum representing HTTP methods.
 */
public enum HttpMethod {
    GET, POST, PUT, DELETE;

    /**
     * Get the HttpMethod from a string.
     *
     * @param value the string to convert
     * @return the HttpMethod
     */
    public static HttpMethod fromString(String value) {
        for (HttpMethod b : HttpMethod.values()) {
            if (b.name().equals(value)) {
                return b;
            }
        }
        return null;
    }
}