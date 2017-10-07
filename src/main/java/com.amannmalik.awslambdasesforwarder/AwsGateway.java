package com.amannmalik.awslambdasesforwarder;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.ses.SESClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AwsGateway {

    public static String pullStringFromS3(String bucket, String key) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        try (S3Client client = S3Client.create()) {
            client.getObject(request,
                    (resp, in) -> {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) != -1) {
                            result.write(buffer, 0, length);
                        }
                        return resp;
                    }
            );
        }

        try {
            return result.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }

    public static void pushStringToSes(String source, List<String> destinations, String emailString) {

        ByteBuffer emailBytes = ByteBuffer.wrap(emailString.getBytes(StandardCharsets.UTF_8));

        RawMessage emailMessage = RawMessage.builder()
                .data(emailBytes)
                .build();

        SendRawEmailRequest request = SendRawEmailRequest.builder()
                .source(source)
                .destinations(destinations)
                .rawMessage(emailMessage)
                .build();

        try (SESClient client = SESClient.create()) {
            client.sendRawEmail(request);
        }

    }


}
