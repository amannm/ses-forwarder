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

                List<String> recipients = ses.getJsonObject("receipt").getJsonArray("recipients").stream()
                        .map(jv -> (JsonString) jv)
                        .map(JsonString::getString)
                        .flatMap(r -> forwardMap.get(r).stream())
                        .collect(Collectors.toList());

                String messageId = mail.getString("messageId");

                AwsGateway.fetchEmailFromS3(bucket, keyPrefix, messageId, emailString -> {
                    String processedEmail = EmailProcessor.execute(emailString, verifiedEmail);
                    AwsGateway.pushEmailToSes(verifiedEmail, recipients, processedEmail);
                });
            }
        }


    }


}