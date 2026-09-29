package com.support.jstackconverter;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonFormatter {

    private final Gson gson;

    public JsonFormatter() {
        // Prints whole numbers without a decimal point (0.0 -> 0, 203.12 stays 203.12)
        JsonSerializer<Float> floatSerializer = (value, type, context) ->
                value == value.longValue()
                        ? new JsonPrimitive(value.longValue())
                        : new JsonPrimitive(value);

        this.gson = new GsonBuilder()
                .disableHtmlEscaping() // keeps frames like "<init>" readable
                .registerTypeAdapter(float.class, floatSerializer)
                .registerTypeAdapter(Float.class, floatSerializer)
                .create();
        // Null fields (e.g. waitingFor, lockedMonitors) are omitted by default
    }

    public void write(JstackDump dump, String outputPath) throws IOException {
        // Build the root object: "version" first, then the dump's "threads"
        JsonObject root = new JsonObject();
        root.addProperty("version", "unknown");
        root.add("threads", gson.toJsonTree(dump).getAsJsonObject().get("threads"));

        try (Writer writer = Files.newBufferedWriter(Paths.get(outputPath), StandardCharsets.UTF_8);
             JsonWriter jsonWriter = new JsonWriter(writer)) {

            jsonWriter.setIndent("\t");
            gson.toJson(root, jsonWriter);
        }
    }

}