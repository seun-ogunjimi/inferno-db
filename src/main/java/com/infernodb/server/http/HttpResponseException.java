package com.infernodb.server.http;

/**
 * Exception to be thrown when an HTTP response should be returned.
 */
public class HttpResponseException extends RuntimeException {

    private final HttpStatus status;
    private final ResponseEntity<?> response;

    public HttpResponseException(HttpStatus status) {
        this.status = status;
        this.response = null;
    }

    public HttpResponseException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.response = null;
    }

    public HttpResponseException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.response = null;
    }

    public HttpResponseException(ResponseEntity<?> responseEntity) {
        this.status = responseEntity.getStatus();
        this.response = responseEntity;
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
     * Get the response entity.
     *
     * @return the response entity
     */
    public ResponseEntity<?> getResponseEntity() {
        return response;
    }
}
