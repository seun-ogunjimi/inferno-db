package com.infernodb.server.http;

/**
 * Enum representing HTTP status codes.
 */
public enum HttpStatus {
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NO_CONTENT(204, "No Content"),
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),
    SEE_OTHER(303, "See Other"),
    NOT_MODIFIED(304, "Not Modified"),
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    PERMANENT_REDIRECT(308, "Permanent Redirect"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    REQUEST_TIMEOUT(408, "Request Timeout"),
    CONFLICT(409, "Conflict"),
    GONE(410, "Gone"),
    LENGTH_REQUIRED(411, "Length Required"),
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout");

    private final int code;
    private final String reason;

    /**
     * Create a new HttpStatus.
     *
     * @param code   the status code
     * @param reason the reason phrase
     */
    HttpStatus(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    /**
     * Get the status code.
     *
     * @return the status code
     */
    public int getCode() {
        return code;
    }

    /**
     * Get the reason phrase.
     *
     * @return the reason phrase
     */
    public String getReason() {
        return reason;
    }

    /**
     * Checks if the status code is in the 2xx range.
     *
     * @return true if the status code is in the 2xx range, false otherwise
     */
    public boolean is2xxSuccessful() {
        return code >= 200 && code < 300;
    }
}
