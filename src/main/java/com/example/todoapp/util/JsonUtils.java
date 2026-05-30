package com.example.todoapp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * JSON serialization/deserialization utility.
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtils() {
    }

    /**
     * Serialize an object to a JSON string.
     *
     * @param o object to serialize
     * @return JSON string
     */
    public static String serialize(Object o) throws JsonProcessingException {
        return MAPPER.writeValueAsString(o);
    }

    /**
     * Deserialize a JSON string into an object of the given type.
     *
     * @param json  JSON string
     * @param clazz target type
     * @param <T>   target type parameter
     * @return deserialized object
     */
    public static <T> T deserialize(String json, Class<T> clazz) throws IOException {
        return MAPPER.readValue(json, clazz);
    }
}
