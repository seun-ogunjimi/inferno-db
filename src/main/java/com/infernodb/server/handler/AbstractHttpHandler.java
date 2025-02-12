package com.infernodb.server.handler;

import com.infernodb.server.http.*;
import com.infernodb.server.utils.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AbstractHttpHandler is an abstract class that provides a skeletal implementation of the HttpHandler interface.
 * <p>
 * AbstractHttpHandler provides helper methods for handling HTTP requests and responses.
 * AbstractHttpHandler also provides a skeletal implementation of the handle method.
 * </p>
 */
public abstract class AbstractHttpHandler implements HttpHandler {
    private final Pattern urlPattern;

    /**
     * Create a new AbstractHttpHandler with the specified URL pattern.
     *
     * @param urlPattern the URL pattern
     */
    protected AbstractHttpHandler(Pattern urlPattern) {
        this.urlPattern = urlPattern;
    }

    /**
     * Handle the incoming HTTP request.
     *
     * @param exchange the HttpExchange object
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var responseEntity = HttpUtils.call(
                () -> handleEndpoints(exchange),
                ex -> handleException(exchange, ex)
        );
        if (responseEntity.isPresent()) {
            var response = responseEntity.get();
            HttpUtils.sendJsonResponse(exchange, response.getStatus(), response.getBody());
        }
    }

    /**
     * Handle the endpoints based on the HTTP method and URL pattern.
     *
     * @param exchange the HttpExchange object
     * @return the response entity
     * @throws IOException if an I/O error occurs
     */
    private ResponseEntity<String> handleEndpoints(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        var endpointMatcher = urlPattern.matcher(path);
        if (!endpointMatcher.matches()) {
            throw new HttpResponseException(HttpStatus.BAD_REQUEST);
        }

        var httpMethod = HttpMethod.fromString(exchange.getRequestMethod());
        if (httpMethod == null) {
            throw new HttpResponseException(HttpStatus.METHOD_NOT_ALLOWED);
        }
        var pathParams = getPathParams(endpointMatcher);
        validate(exchange, pathParams);
        // Handle the request based on the method
        return switch (httpMethod) {
            case HttpMethod.GET -> handleGet(exchange, pathParams);
            case HttpMethod.PUT -> handlePut(exchange, pathParams);
            case HttpMethod.DELETE -> handleDelete(exchange, pathParams);
            default -> throw new HttpResponseException(HttpStatus.METHOD_NOT_ALLOWED);
        };
    }

    /**
     * Get the logger for this class.
     *
     * @return the logger
     */
    protected Logger getLogger() {
        return HttpUtils.LOGGER;
    }

    /**
     * Get the path parameters from the URL pattern.
     *
     * @param matcher the matcher object
     * @return a map of path parameters
     */
    protected Map<String, String> getPathParams(Matcher matcher) {
        var params = new HashMap<String, String>();
        for (var name : urlPattern.namedGroups().keySet()) {
            params.put(name, matcher.group(name));
        }
        return params;
    }

    protected void validate(HttpExchange exchange , Map<String, String> pathParams) throws HttpResponseException {
        // Validate the request
    }

    /**
     * Handle the exception by sending an HTTP response.
     *
     * @param exchange the HttpExchange object
     * @param ex       the exception
     */
    private void handleException(HttpExchange exchange, HttpResponseException ex) {
        try {
            var responseEntity = ex.getResponseEntity();
            var httpStatus = responseEntity != null ? responseEntity.getStatus() : ex.getStatus();
            var message = responseEntity != null && responseEntity.getBody() != null ? responseEntity.getBody().toString() : null;
            if (message == null || message.isBlank()) {
                message = JsonParser.toHttpStatusJson(httpStatus, ex.getMessage());
            }
            HttpUtils.sendJsonResponse(exchange, httpStatus, message);
        } catch (Exception e) {
            getLogger().severe("Error sending response: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Handle a GET request.
     *
     * @param exchange   the HttpExchange object
     * @param pathParams the path parameters
     * @return the response entity
     * @throws IOException if an I/O error occurs
     */
    public abstract ResponseEntity<String> handleGet(HttpExchange exchange, Map<String, String> pathParams) throws IOException;

    /**
     * Handle a PUT request.
     *
     * @param exchange   the HttpExchange object
     * @param pathParams the path parameters
     * @return the response entity
     * @throws IOException if an I/O error occurs
     */
    public abstract ResponseEntity<String> handlePut(HttpExchange exchange, Map<String, String> pathParams) throws IOException;

    /**
     * Handle a DELETE request.
     *
     * @param exchange   the HttpExchange object
     * @param pathParams the path parameters
     * @return the response entity
     * @throws IOException if an I/O error occurs
     */
    public abstract ResponseEntity<String> handleDelete(HttpExchange exchange, Map<String, String> pathParams) throws IOException;

}
