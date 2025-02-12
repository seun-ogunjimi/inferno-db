package com.infernodb.server.http;

/**
 * Represents an HTTP response entity.
 *
 * @param <T> the type of the response body
 */
public class ResponseEntity<T> {
    private final HttpStatus status;
    private final T body;

    public ResponseEntity(HttpStatus status) {
        this(status, null);
    }

    /**
     * Create a new response entity with the given status and body.
     *
     * @param status the status of the response
     * @param body   the body of the response
     */
    public ResponseEntity(HttpStatus status, T body) {
        this.status = status;
        this.body = body;
    }

    /**
     * Get the HTTP status of the response.
     *
     * @return the HTTP status
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Get the body of the response.
     *
     * @return the body
     */
    public T getBody() {
        return body;
    }
}
