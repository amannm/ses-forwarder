package com.amannmalik.awslambdasesforwarder;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.ses.SESClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

public class AwsGateway {

    public static void fetchEmailFromS3(String bucket, String keyPrefix, String messageId, Consumer<String> handler) {
        S3Client client = S3Client.create();
        client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(keyPrefix + messageId)
                        .build(),
                (resp, in) -> {
                    String emailString = loadEmailContent(in);
                    handler.accept(emailString);
                    return resp;
                }
        );

    }

    public static void pushEmailToSes(String originalRecipient, List<String> recipients, String emailString) {
        ByteBuffer emailBytes = ByteBuffer.wrap(emailString.getBytes(StandardCharsets.UTF_8));
        SESClient client = SESClient.create();
        SendRawEmailResponse sendRawEmailResponse = client.sendRawEmail(SendRawEmailRequest.builder()
                .destinations(recipients)
                .source(originalRecipient)
                .rawMessage(RawMessage.builder()
                        .data(emailBytes)
                        .build())
                .build());
    }

    private static String loadEmailContent(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }

}
