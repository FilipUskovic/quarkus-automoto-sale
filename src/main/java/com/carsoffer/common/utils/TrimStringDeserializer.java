package com.carsoffer.common.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class TrimStringDeserializer extends StdDeserializer<String> {
    public TrimStringDeserializer() {
        this(null);
    }

    public TrimStringDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public String deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        String value = jsonParser.getValueAsString();
        return value != null ? value.trim() : null;
    //    return value != null ? value.trim().replaceAll("\\s+", "") : null;

    }
}
