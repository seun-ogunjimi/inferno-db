package com.infernodb.server.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HttpUtils is a utility class that provides helper methods for handling HTTP requests and responses.
 */
public interface HttpUtils {
    Logger LOGGER = Logger.getLogger(HttpUtils.class.getName());

    /**
     * Reads the request body from the HttpExchange object.
     *
     * @param exchange the HttpExchange object
     * @return the request body as a string
     * @throws IOException if an I/O error occurs
     */
    static String readRequestBody(HttpExchange exchange) throws IOException {
        var inputStream = exchange.getRequestBody();
        int contentLength = getContentLength(exchange);
        contentLength = contentLength > 0 && contentLength < 1024 * 4 ? contentLength : 1024;
        try (var byteArrayOutputStream = new ByteArrayOutputStream()) {
            var buffer = new byte[contentLength];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            // Convert ByteArrayOutputStream to a byte array
            return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * Gets the content length from the HttpExchange object.
     *
     * @param exchange the HttpExchange object
     * @return the content length
     */
    static int getContentLength(HttpExchange exchange) {
        try {
            var contentLengthHeader = exchange.getRequestHeaders().get("Content-Length");
            return contentLengthHeader != null && !contentLengthHeader.isEmpty()
                    ? Integer.parseInt(contentLengthHeader.getFirst())
                    : -1;
        } catch (Exception e) {
            LOGGER.info("Invalid Content-Length header");
        }
        return -1;
    }

    /**
     * Sends a text response to the HttpExchange object.
     *
     * @param exchange    the HttpExchange object
     * @param httpStatus  the HTTP status code
     * @param textMessage the text message
     * @throws IOException if an I/O error occurs
     */
    static void sendTextResponse(HttpExchange exchange, HttpStatus httpStatus, String textMessage) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        var message = textMessage == null ? httpStatus.getReason() : textMessage;
        exchange.sendResponseHeaders(httpStatus.getCode(), message.length());
        try (var os = exchange.getResponseBody()) {
            os.write(message.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Sends a JSON response to the HttpExchange object.
     *
     * @param exchange     the HttpExchange object
     * @param httpStatus   the HTTP status code
     * @param jsonResponse the JSON response
     * @throws IOException if an I/O error occurs
     */
    static void sendJsonResponse(HttpExchange exchange, HttpStatus httpStatus, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(httpStatus.getCode(), jsonResponse.length());
        try (var os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Parses the query parameters from the URI.
     *
     * @param query the query string
     * @return a map of query parameters
     */
    static Map<String, String> parseQueryParams(String query) {
        var params = new HashMap<String, String>();
        if (query != null) {
            query = query.trim();
            for (var param : query.split("&")) {
                var keyValue = param.split("=");
                if (keyValue.length == 2) {
                    var key = decodeUrl(keyValue[0]);
                    var value = decodeUrl(keyValue[1]);
                    params.put(key, value);
                }
            }
        }
        return params;
    }

    /**
     * Decodes a URL-encoded string.
     *
     * @param value the URL-encoded string
     * @return the decoded string
     */
    static String decodeUrl(String value) {
        return value != null ? URLDecoder.decode(value, StandardCharsets.UTF_8) : null;
    }

    /**
     * Calls the specified callable and handles the response.
     *
     * @param callable the callable object
     * @param onerror  the error handler
     * @param <T>      the response type
     * @return an optional response entity
     */
    static <T> Optional<ResponseEntity<T>> call(Callable<ResponseEntity<T>> callable, Consumer<HttpResponseException> onerror) {
        try {
            var responseEntity = callable.call();
            if (responseEntity == null) {
                throw new HttpResponseException(HttpStatus.BAD_REQUEST, "Invalid response");
            }
            if (responseEntity.getStatus().is2xxSuccessful()) {
                return Optional.of(responseEntity);
            }
            throw new HttpResponseException(responseEntity);
        } catch (Exception e) {
            Optional.ofNullable(onerror)
                    .orElse(ex -> LOGGER.log(Level.SEVERE, "Error in Http Handler: %s".formatted(ex.getMessage()), ex))
                    .accept(e instanceof HttpResponseException ex ? ex : new HttpResponseException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
        return Optional.empty();
    }
}
