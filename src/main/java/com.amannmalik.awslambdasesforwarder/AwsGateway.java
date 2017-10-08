package com.amannmalik.awslambdasesforwarder;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AwsGateway {

    public static String pullStringFromS3(String bucket, String key) {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
            S3Object o = s3.getObject(bucket, key);
            S3ObjectInputStream s3is = o.getObjectContent();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = s3is.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            s3is.close();
            return result.toString(StandardCharsets.UTF_8.name());
        } catch (AmazonServiceException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void pushStringToSes(String source, List<String> destinations, String emailString) {
        ByteBuffer emailBytes = ByteBuffer.wrap(emailString.getBytes(StandardCharsets.UTF_8));
        RawMessage emailMessage = new RawMessage(emailBytes);
        SendRawEmailRequest request = new SendRawEmailRequest()
                .withSource(source)
                .withDestinations(destinations)
                .withRawMessage(emailMessage);
        try {
            AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.defaultClient();
            client.sendRawEmail(request);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
