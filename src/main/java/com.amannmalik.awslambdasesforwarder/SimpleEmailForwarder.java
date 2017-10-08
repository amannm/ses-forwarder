package com.amannmalik.awslambdasesforwarder;

import java.util.Collections;
import java.util.List;

public class SimpleEmailForwarder {

    private String sourceEmailAddress;
    private String destinationEmailAddress;
    private String emailBucket;
    private String emailBucketKeyPrefix;
    private String emailMessageId;

    public void setSourceEmailAddress(String sourceEmailAddress) {
        this.sourceEmailAddress = sourceEmailAddress;
    }

    public void setDestinationEmailAddress(String destinationEmailAddress) {
        this.destinationEmailAddress = destinationEmailAddress;
    }

    public void setEmailBucket(String emailBucket) {
        this.emailBucket = emailBucket;
    }

    public void setEmailBucketKeyPrefix(String emailBucketKeyPrefix) {
        this.emailBucketKeyPrefix = emailBucketKeyPrefix;
    }

    public void setEmailMessageId(String emailMessageId) {
        this.emailMessageId = emailMessageId;
    }

    public void execute() {
        System.out.println("execution started");

        List<String> newRecipients = Collections.singletonList(destinationEmailAddress);
        String emailBucketKey = emailBucketKeyPrefix + emailMessageId;

        System.out.println(String.format("pulling email %s from %s/%s", emailMessageId, emailBucket, emailBucketKeyPrefix));
        String originalEmail = AwsGateway.pullStringFromS3(emailBucket, emailBucketKey);

        System.out.println("rewriting email");
        String processedEmail = EmailRewriter.process(originalEmail, sourceEmailAddress);

        System.out.println(String.format("pushing rewritten email %s as %s to %s", emailMessageId, sourceEmailAddress, destinationEmailAddress));
        AwsGateway.pushStringToSes(sourceEmailAddress, newRecipients, processedEmail);

        System.out.println("execution success");
    }

}
