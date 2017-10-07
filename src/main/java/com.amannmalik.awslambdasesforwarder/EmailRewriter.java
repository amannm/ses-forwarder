package com.amannmalik.awslambdasesforwarder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailRewriter {

    public static String process(String emailString, String verifiedEmail) {
        String header;
        String body;
        Pattern emailPattern = Pattern.compile("^((?:.+\\r?\\n)*)(\\r?\\n(?:.*\\s+)*)", Pattern.MULTILINE);
        Matcher matcher = emailPattern.matcher(emailString);
        if (matcher.matches()) {
            header = matcher.group(1);
            body = matcher.group(2);

            //TODO: sure about this behavior?
            if (header == null) {
                header = emailString;
            }
            if (body == null) {
                body = "";
            }

        } else {
            header = emailString;
            body = "";
        }

        header = rewriteFrom(header, verifiedEmail);
        header = stripInvalidHeaders(header);

        return header + body;
    }

    private static String rewriteFrom(String header, String verifiedEmail) {

        Pattern fromHeaderPattern = Pattern.compile("^From: (.*(?:\\r?\\n\\s+.*)*)(\\r?\\n)", Pattern.MULTILINE);
        Matcher matcher = fromHeaderPattern.matcher(header);

        //TODO: multiple 'From: ' possible?

        if (!matcher.find()) {
            throw new RuntimeException("'From: ' header not found");
        }

        String fromHeaderValue = matcher.group(1);
        if (fromHeaderValue == null) {
            throw new RuntimeException("'From: ' header value not found");
        }

        String fromHeaderLineEnd = matcher.group(2);
        if (fromHeaderLineEnd == null) {
            throw new RuntimeException("'From: ' header line ending not found");
        }

        // ensure the original sender's email in the 'From:' header is overwritten with a verified SES email
        String fromName = fromHeaderValue.replace("<(.*)>", "").trim();
        String modifiedFromHeader = "From: " + fromName + " <" + verifiedEmail + ">" + fromHeaderLineEnd;
        String modifiedHeader = matcher.replaceFirst(modifiedFromHeader);

        // ensure the original sender's name and email exists in the 'Reply-To:' header
        matcher = Pattern.compile("^Reply-To: ", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(header);
        if (!matcher.find()) {
            modifiedHeader = modifiedHeader + "Reply-To: " + fromHeaderValue + fromHeaderLineEnd;
        }

        return modifiedHeader;
    }

    private static String stripInvalidHeaders(String header) {

        Matcher matcher = Pattern.compile("^Return-Path: (.*)\\r?\\n", Pattern.MULTILINE).matcher(header);
        if (matcher.find()) {
            header = matcher.replaceAll("");
        }

        matcher = Pattern.compile("^Sender: (.*)\\r?\\n", Pattern.MULTILINE).matcher(header);
        if (matcher.find()) {
            header = matcher.replaceAll("");
        }

        matcher = Pattern.compile("^Message-ID: (.*)\\r?\\n", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(header);
        if (matcher.find()) {
            header = matcher.replaceAll("");
        }

        matcher = Pattern.compile("^DKIM-Signature: .*\\r?\\n(\\s+.*\\r?\\n)*", Pattern.MULTILINE).matcher(header);
        if (matcher.find()) {
            header = matcher.replaceAll("");
        }

        return header;
    }
}
