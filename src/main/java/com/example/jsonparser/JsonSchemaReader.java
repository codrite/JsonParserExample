package com.example.jsonparser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JsonSchemaReader {

    public JsonSchemaReader() {}

    public static Map<String, Object> readSchema(String schemaSourceAsJson) throws JsonProcessingException {
        return new ObjectMapper().reader().forType(Map.class).readValue(schemaSourceAsJson);
    }
}
