package com.amannmalik.awslambdasesforwarder;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

public class SimpleEmailForwarder {

    private static final Logger LOG = Logger.getLogger(SimpleEmailForwarder.class);

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

        List<String> newRecipients = Collections.singletonList(destinationEmailAddress);
        String emailBucketKey = emailBucketKeyPrefix + emailMessageId;

        String originalEmail = AwsGateway.pullStringFromS3(emailBucket, emailBucketKey);
        LOG.info(String.format("pulled email %s from %s/%s", emailMessageId, emailBucket, emailBucketKeyPrefix));

        String processedEmail = EmailRewriter.process(originalEmail, sourceEmailAddress);

        AwsGateway.pushStringToSes(sourceEmailAddress, newRecipients, processedEmail);
        LOG.info(String.format("pushed rewritten email %s as %s to %s", emailMessageId, sourceEmailAddress, destinationEmailAddress));
    }

}
