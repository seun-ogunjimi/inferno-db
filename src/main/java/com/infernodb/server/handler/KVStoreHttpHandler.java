package com.infernodb.server.handler;

import com.infernodb.server.http.HttpResponseException;
import com.infernodb.server.http.HttpStatus;
import com.infernodb.server.http.HttpUtils;
import com.infernodb.server.http.ResponseEntity;
import com.infernodb.server.storage.KVBucket;
import com.infernodb.server.storage.KVStore;
import com.infernodb.server.utils.JsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An HTTP handler for the key-value store.
 * <p>
 * The handler supports the following endpoints:
 * <ul>
 *     <li>GET /inferno/{bucket}/keys/{key}</li>
 *     <li>PUT /inferno/{bucket}/keys/{key} -d {"key":"{key}","value":"{value}"} </li>
 *     <li>DELETE /inferno/{bucket}/keys/{key}</li>
 *     <li>GET /inferno/{bucket}/keys?startKey={startKey}&endKey={endKey}</li>
 *     <li>PUT /inferno/{bucket}/keys -d [{"key":"{key}","value":"{value}"}, ...]</li>
 * </ul>
 * The handler supports the following endpoints for default bucket:
 * <ul>
 *     <li>GET /inferno/keys/{key}</li>
 *     <li>PUT /inferno/keys/{key} -d {"key":"{key}","value":"{value}"}</li>
 *     <li>DELETE /inferno/keys/{key}</li>
 *     <li>GET /inferno/keys?startKey={startKey}&endKey={endKey}</li>
 *     <li>PUT /inferno/keys -d [{"key":"{key}","value":"{value}"}, ...]</li>
 * </ul>
 * The handler uses the following status codes:
 * <ul>
 *     <li>200 OK: the request was successful</li>
 *     <li>201 CREATED: the resource was created</li>
 *     <li>204 NO CONTENT: the resource was deleted</li>
 *     <li>400 BAD REQUEST: the request was invalid</li>
 *     <li>404 NOT FOUND: the resource was not found</li>
 *     <li>405 METHOD NOT ALLOWED: the method is not allowed</li>
 *     <li>500 INTERNAL SERVER ERROR: an error occurred while processing the request</li>
 * </ul>
 * </p>
 *
 * <p>
 *     The handler uses the following JSON format for key-value pairs:
 * </p>
 * <pre>
 *             {
 *             "key": "key",
 *             "value": "value"
 *             }
 * </pre>
 * <p>
 *     The handler uses the following JSON format for key-value pair arrays:
 * </p>
 * <pre>
 *                	[
 *                                                {
 *                		"key": "key1",
 *                		"value": "value1"
 *                        },
 *                        {
 *                		"key": "key2",
 *                		"value": "value2"
 *                        },
 *                		...
 *                	]
 *
 * </pre>
 * <p>
 *     The handler uses the following JSON format for HTTP status codes:
 * </p>
 * <pre>
 *                        {
 *                        "code": 200,
 *                        "message": "OK"
 *                        }
 *
 * </pre>
 *
 * @see KVStore
 * @see KVBucket
 * @see JsonParser
 * @see HttpUtils
 * @see ResponseEntity
 * @see HttpStatus
 * @see HttpExchange
 */
public class KVStoreHttpHandler extends AbstractHttpHandler {
    public static final String PATH = "/inferno";
    private static final String KEY_PARAM = "key";
    private static final String BUCKET_PARAM = "bucket";
    private static final Pattern URL_PATTERN = Pattern.compile("^" + PATH + "(?:/(?<" + BUCKET_PARAM + ">[^/]+))?/keys(?:/(?<" + KEY_PARAM + ">[^/]+))?(?:\\?.*)?$");

    private static final Logger LOGGER = Logger.getLogger(KVStoreHttpHandler.class.getName());
    private final KVStore kvStore;
    private final Pattern paramPattern = Pattern.compile("^[a-zA-Z0-9_-]+$");

    /**
     * Create a new handler for the specified KV store.
     *
     * @param kvStore the key-value store
     */
    public KVStoreHttpHandler(KVStore kvStore) {
        super(URL_PATTERN);
        Objects.requireNonNull(kvStore);
        this.kvStore = kvStore;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    /**
     * Get the bucket with the specified name. If the bucket does not exist, it will be created.
     *
     * @param bucketName the name of the bucket
     * @return the bucket with the specified name
     * @throws IOException if an error occurs while creating the bucket
     */
    private KVBucket getBucket(String bucketName) throws IOException {
        var bucket = kvStore.getBucket(bucketName);
        if (bucket == null) {
            bucket = kvStore.createBucket(bucketName);
        }
        return bucket;
    }

    protected void validate(HttpExchange exchange , Map<String, String> pathParams) throws HttpResponseException {
       var bucket = pathParams.get(BUCKET_PARAM);
        if (bucket!=null && !paramPattern.matcher(bucket).matches()) {
            throw new HttpResponseException(HttpStatus.BAD_REQUEST, "Invalid bucket name");
        }
    }

    //*********************CODE_SNIPPET_FOR_ENDPOINTS************************

    /**
     * Get the value of the key in the specified bucket.
     *
     * @param exchange the HTTP exchange
     * @param bucket   the name of the bucket
     * @param key      the key
     * @return the response entity
     * @throws IOException if an error occurs while processing the request
     */
    public ResponseEntity<String> get(HttpExchange exchange, String bucket, String key) throws IOException {
        var value = getBucket(bucket).get(key);
        if (value != null) {
            return new ResponseEntity<>(HttpStatus.OK, JsonParser.toKeyValueJson(key, value));
        }
        throw new HttpResponseException(HttpStatus.NOT_FOUND, "Key not found");
    }

    /**
     * Put the key-value pair in the specified bucket.
     *
     * @param exchange the HTTP exchange
     * @param bucket   the name of the bucket
     * @param key      the key
     * @return the response entity
     * @throws IOException if an error occurs while processing the request
     */
    public ResponseEntity<String> put(HttpExchange exchange, String bucket, String key) throws IOException {
        var requestBody = HttpUtils.readRequestBody(exchange);
        var kv = JsonParser.fromKeyValueJson(requestBody);
        if (kv.getKey() == null) {
            throw new HttpResponseException(HttpStatus.BAD_REQUEST, "Key is required in request body");
        }
        if (!kv.getKey().equals(key)) {
            throw new HttpResponseException(HttpStatus.BAD_REQUEST, "Key in request body does not match key in URL");
        }
        if (kv.getValue() == null) {
            throw new HttpResponseException(HttpStatus.BAD_REQUEST, "Value is required in request body");
        }
        getBucket(bucket).put(kv.getKey(), kv.getValue());
        return new ResponseEntity<>(HttpStatus.OK, JsonParser.toHttpStatusJson(HttpStatus.OK));
    }

    /**
     * Delete the key in the specified bucket.
     *
     * @param exchange the HTTP exchange
     * @param bucket   the name of the bucket
     * @param key      the key
     * @return the response entity
     * @throws IOException if an error occurs while processing the request
     */
    public ResponseEntity<String> delete(HttpExchange exchange, String bucket, String key) throws IOException {
        getBucket(bucket).delete(key);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT, JsonParser.toHttpStatusJson(HttpStatus.NO_CONTENT));
    }

    /**
     * Get the key-value pairs in the specified range in the specified bucket.
     *
     * @param exchange the HTTP exchange
     * @param bucket   the name of the bucket
     * @return the response entity
     * @throws IOException if an error occurs while processing the request
     */
    public ResponseEntity<String> getRange(HttpExchange exchange, String bucket) throws IOException {
        var queryMap = HttpUtils.parseQueryParams(exchange.getRequestURI().getQuery());
        var startKey = queryMap.get("startKey");
        var endKey = queryMap.get("endKey");
        if (startKey == null && endKey == null) {
            throw new HttpResponseException(HttpStatus.BAD_REQUEST, "startKey and endKey query parameters are required");
        }
        var kvBucket = getBucket(bucket);
        var kvMap = kvBucket.get(startKey, endKey);
        if (kvMap != null) {
            return new ResponseEntity<>(HttpStatus.OK, JsonParser.toKeyValueJsonArray(kvMap));
        }
        throw new HttpResponseException(HttpStatus.NOT_FOUND, "Keys not found");
    }

    /**
     * Put the key-value pairs in the specified bucket.
     *
     * @param exchange the HTTP exchange
     * @param bucket   the name of the bucket
     * @return the response entity
     * @throws IOException if an error occurs while processing the request
     */
    public ResponseEntity<String> putBatch(HttpExchange exchange, String bucket) throws IOException {
        var requestBody = HttpUtils.readRequestBody(exchange);
        var kvMap = JsonParser.fromKeyValueJsonArray(requestBody);
        getBucket(bucket).put(kvMap);
        return new ResponseEntity<>(HttpStatus.OK, JsonParser.toHttpStatusJson(HttpStatus.OK));
    }

    //*********************OVERRIDE HANDLER METHODS************************

    /**
     * Handle the GET request.
     *
     * @param exchange   the HTTP exchange
     * @param pathParams the path parameters
     * @return the response entity
     * @throws IOException if an error occurs while processing the request
     */
    @Override
    public ResponseEntity<String> handleGet(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        var key = pathParams.get(KEY_PARAM);
        var bucket = pathParams.get(BUCKET_PARAM);
        if (key != null) {
            return get(exchange, bucket, key);
        } else {
            return getRange(exchange, bucket);
        }
    }

    /**
     * Handle the PUT request.
     *
     * @param exchange   the HTTP exchange
     * @param pathParams the path parameters
     * @return the response entity
     * @throws IOException if an error occurs while processing the request
     */
    @Override
    public ResponseEntity<String> handlePut(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        var key = pathParams.get(KEY_PARAM);
        var bucket = pathParams.get(BUCKET_PARAM);
        if (key != null) {
            return put(exchange, bucket, key);
        } else {
            return putBatch(exchange, bucket);
        }
    }

    /**
     * Handle the DELETE request.
     *
     * @param exchange   the HTTP exchange
     * @param pathParams the path parameters
     * @return the response entity
     * @throws IOException if an error occurs while processing the request
     */
    @Override
    public ResponseEntity<String> handleDelete(HttpExchange exchange, Map<String, String> pathParams) throws IOException {
        var key = pathParams.get(KEY_PARAM);
        var bucket = pathParams.get(BUCKET_PARAM);
        if (key != null) {
            return delete(exchange, bucket, key);
        }
        throw new HttpResponseException(HttpStatus.BAD_REQUEST, "Key is required");
    }
}
