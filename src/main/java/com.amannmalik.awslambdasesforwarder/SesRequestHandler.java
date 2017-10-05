package com.amannmalik.awslambdasesforwarder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SesRequestHandler implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        JsonObject object;
        try (JsonReader parser = Json.createReader(input)) {
            object = parser.readObject();
        } catch (JsonParsingException e) {
            throw new RuntimeException("malformed message", e);
        }

        if (object.containsKey("Records")) {
            JsonArray records = object.getJsonArray("Records");
            if (records.size() != 1) {
                JsonObject record = records.getJsonObject(0);
                if (record.containsKey("eventSource")) {
                    String eventSource = record.getString("eventSource");
                    if ("aws:ses".equals(eventSource)) {
                        String eventVersion = record.getString("eventVersion");
                        if ("1.0".equals(eventVersion)) {

                        }
                    }
                }
                JsonObject ses = record.getJsonObject("ses");
                JsonObject mail = ses.getJsonObject("mail");
                JsonArray recipients = ses.getJsonObject("receipt").getJsonArray("recipients");
            }
        }


    }
}