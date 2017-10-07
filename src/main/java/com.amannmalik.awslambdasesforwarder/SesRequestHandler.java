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

        String destinationEmailAddress = System.getenv("DESTINATION_EMAIL_ADDRESS");
        if (destinationEmailAddress == null) {
            throw new RuntimeException("environment variable 'DESTINATION_EMAIL_ADDRESS' is required");
        }

        String sourceEmailAddress = System.getenv("SOURCE_EMAIL_ADDRESS");
        if (sourceEmailAddress == null) {
            throw new RuntimeException("environment variable 'SOURCE_EMAIL_ADDRESS' is required");
        }

        String bucket = System.getenv("EMAIL_BUCKET");
        if (bucket == null) {
            throw new RuntimeException("environment variable 'EMAIL_BUCKET' is required");
        }

        String keyPrefix = System.getenv("EMAIL_BUCKET_KEY_PREFIX");
        if (keyPrefix == null) {
            keyPrefix = "";
        }

        JsonObject ses = extractSesEvent(input);

        String messageId = ses.getJsonObject("mail").getString("messageId");

        SimpleEmailForwarder forwarder = new SimpleEmailForwarder();
        forwarder.setSourceEmailAddress(sourceEmailAddress);
        forwarder.setDestinationEmailAddress(destinationEmailAddress);
        forwarder.setEmailBucket(bucket);
        forwarder.setEmailBucketKeyPrefix(keyPrefix);
        forwarder.setEmailMessageId(messageId);

        forwarder.execute();

    }

    private static JsonObject extractSesEvent(InputStream input) {

        JsonObject object;
        try (JsonReader parser = Json.createReader(input)) {
            object = parser.readObject();
        } catch (JsonParsingException e) {
            throw new RuntimeException("malformed request", e);
        }

        if (!object.containsKey("Records")) {
            throw new RuntimeException("malformed SES event object");
        }

        JsonArray records = object.getJsonArray("Records");
        if (records.size() != 1) {
            throw new RuntimeException("malformed SES event object");
        }

        JsonObject record = records.getJsonObject(0);
        if (!record.containsKey("eventSource")) {
            throw new RuntimeException("malformed SES event object");
        }

        String eventSource = record.getString("eventSource");
        if (!"aws:ses".equals(eventSource)) {
            throw new RuntimeException("malformed SES event object");
        }

        String eventVersion = record.getString("eventVersion");
        if (!"1.0".equals(eventVersion)) {
            throw new RuntimeException("malformed SES event object");
        }

        return record.getJsonObject("ses");
    }


}