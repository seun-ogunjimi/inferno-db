package com.infernodb.server.utils;

import com.infernodb.server.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonParserTest {

    @Test
    void toKeyValueJson() {
        // Given
        var key = "key1";
        var value = "value1";
        var expectedJson = "{\"key\":\"key1\",\"value\":\"value1\"}";

        // When
        var actualJson = JsonParser.toKeyValueJson(key, value);

        // Then
        assertEquals(expectedJson, actualJson);
    }

    @Test
    void testToKeyValueJson() {
        // Given
        var key = "key1";
        var value = "value1";
        var expectedJson = "{\"key\":\"key1\",\"value\":\"value1\"}";

        // When
        var actualJson = JsonParser.toKeyValueJson(new AbstractMap.SimpleImmutableEntry<>(key, value));

        // Then
        assertEquals(expectedJson, actualJson);
    }

    @Test
    void fromKeyValueJson() {
        // Given
        var json = "{\"key\":\"key1\",\"value\":\"value1\"}";
        var expectedEntry = new AbstractMap.SimpleImmutableEntry<>("key1", "value1");

        // When
        var actualEntry = JsonParser.fromKeyValueJson(json);

        // Then
        assertEquals(expectedEntry, actualEntry);
    }

    @Test
    void toKeyValueJsonArray() {
        // Given
        var kvMap = new LinkedHashMap<String,String>();
        kvMap.put("key1", "value1");
        kvMap.put("key2", "value2");
        var expectedJson = "[{\"key\":\"key1\",\"value\":\"value1\"},{\"key\":\"key2\",\"value\":\"value2\"}]";
        // When
        var actualJson = JsonParser.toKeyValueJsonArray(kvMap);
        // Then
        assertEquals(expectedJson, actualJson);
    }

    @Test
    void fromKeyValueJsonArray() {
        // Given
        var json = "[{\"key\":\"key1\",\"value\":\"value1\"},{\"key\":\"key2\",\"value\":\"value2\"}]";
        var expectedMap = new HashMap<String, String>();
        expectedMap.put("key1", "value1");
        expectedMap.put("key2", "value2");

        // When
        var actualMap = JsonParser.fromKeyValueJsonArray(json);

        // Then
        assertEquals(expectedMap, actualMap);
    }

    @Test
    void toHttpStatusJson() {
        // Given
        var status = HttpStatus.OK;
        var expectedPathJson = "\"status\":\""+status.getCode()+"\",\"message\":\""+status.getReason()+"\"";

        // When
        var actualJson = JsonParser.toHttpStatusJson(status);

        // Then
        assertTrue(actualJson.contains(expectedPathJson));
    }

    @Test
    void testToHttpStatusJson() {
        // Given
        var status = HttpStatus.OK;
        var message = "Test message";
        var expectedPathJson = "\"status\":\""+status.getCode()+"\",\"message\":\""+message+"\"";

        // When
        var actualJson = JsonParser.toHttpStatusJson(status, message);

        // Then
        assertTrue(actualJson.contains(expectedPathJson));
    }
}