package com.amannmalik.awslambdasesforwarder;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import javax.json.*;
import javax.json.stream.JsonParsingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SesRequestHandler implements RequestStreamHandler {

    private static Map<String, Set<String>> forwardMap = new HashMap<>();
    private static String verifiedEmail = "";
    private static String bucket = "";
    private static String keyPrefix = "";

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        JsonObject object;
        try (JsonReader parser = Json.createReader(input)) {
            object = parser.readObject();
        } catch (JsonParsingException e) {
            throw new RuntimeException("malformed message", e);
        }

        if (!object.containsKey("Records")) {
            return;
        }

        JsonArray records = object.getJsonArray("Records");
        if (records.size() != 1) {
            return;
        }

        JsonObject record = records.getJsonObject(0);
        if (!record.containsKey("eventSource")) {
            return;
        }

        String eventSource = record.getString("eventSource");
        if (!"aws:ses".equals(eventSource)) {
            return;
        }

        String eventVersion = record.getString("eventVersion");
        if (!"1.0".equals(eventVersion)) {
            return;
        }

        JsonObject ses = record.getJsonObject("ses");

        List<String> recipients = ses.getJsonObject("receipt").getJsonArray("recipients").stream()
                .map(jv -> (JsonString) jv)
                .map(JsonString::getString)
                .collect(Collectors.toList());

        String messageId = ses.getJsonObject("mail").getString("messageId");

        forward(messageId, recipients);
    }

    private static List<String> mapRecipients(List<String> originalRecipients) {
        return originalRecipients.stream()
                .flatMap(r -> forwardMap.get(r).stream())
                .collect(Collectors.toList());
    }

    private static void forward(String messageId, List<String> recipients) {
        AwsGateway.fetchEmailFromS3(bucket, keyPrefix, messageId, emailString -> {
            String processedEmail = EmailRewriter.process(emailString, verifiedEmail);
            List<String> newRecipients = mapRecipients(recipients);
            AwsGateway.pushEmailToSes(verifiedEmail, newRecipients, processedEmail);
        });
    }


}