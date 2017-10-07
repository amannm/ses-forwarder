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
import java.util.Collections;
import java.util.List;

public class SesRequestHandler implements RequestStreamHandler {

    //private static Map<String, Set<String>> forwardMap = new HashMap<>();
    private static String destinationEmailAddress = System.getenv("DESTINATION_EMAIL_ADDRESS");
    private static String sourceEmailAddress = System.getenv("SOURCE_EMAIL_ADDRESS");
    private static String bucket = System.getenv("EMAIL_BUCKET");
    private static String keyPrefix = System.getenv("EMAIL_BUCKET_KEY_PREFIX");

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {

        JsonObject ses = extractSesEvent(input);

        String messageId = ses.getJsonObject("mail").getString("messageId");

        //TODO: allow more complex mapping than [* -> 1]
        /*
                List<String> recipients = ses.getJsonObject("receipt").getJsonArray("recipients").stream()
                .map(v -> (JsonString) v)
                .map(JsonString::getString)
                .collect(Collectors.toList());
                List<String> newRecipients = mapRecipients(recipients)
         */

        List<String> newRecipients = Collections.singletonList(destinationEmailAddress);

        String originalEmail = AwsGateway.fetchEmailFromS3(bucket, keyPrefix, messageId);
        String processedEmail = EmailRewriter.process(originalEmail, sourceEmailAddress);
        AwsGateway.pushEmailToSes(sourceEmailAddress, newRecipients, processedEmail);
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

//    private static List<String> mapRecipients(List<String> originalRecipients) {
//        return originalRecipients.stream()
//                .flatMap(r -> forwardMap.get(r).stream())
//                .collect(Collectors.toList());
//    }



}