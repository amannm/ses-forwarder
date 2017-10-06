package com.amannmalik.awslambdasesforwarder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailRewriter {

    public static String process(String emailString, String verifiedEmail) {
        Pattern headerPattern = Pattern.compile("^((?:.+\\r?\\n)*)(\\r?\\n(?:.*\\s+)*)", Pattern.MULTILINE);
        Matcher matcher = headerPattern.matcher(emailString);
        String header = matcher.group(1);
        String body = matcher.group(2);

        header = ensureReplyToExists(header);
        header = replaceFromWithVerified(header, verifiedEmail);
        header = stripInvalidHeaders(header);

        return header + body;
    }

    private static String ensureReplyToExists(String header) {
        Pattern replyToPattern = Pattern.compile("^Reply-To: ", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        if (!replyToPattern.matcher(header).matches()) {
            Pattern compile = Pattern.compile("^From: (.*(?:\\r?\\n\\s+.*)*\\r?\\n)", Pattern.MULTILINE);
            String fromValue = compile.matcher(header).group(1);
            header = header + "Reply-To: " + fromValue;
        }
        return header;
    }

    private static String replaceFromWithVerified(String header, String verifiedEmail) {
        Pattern returnPathPattern = Pattern.compile("^From: (.*(?:\\r?\\n\\s+.*)*)", Pattern.MULTILINE);
        Matcher matcher = returnPathPattern.matcher(header);
        String fromValue = matcher.group(1);
        String fromName = fromValue.replace("<(.*)>", "").trim();
        String fromHeader = "From: " + fromName + " <" + verifiedEmail + ">";
        return matcher.replaceAll(fromHeader);
    }

    private static String stripInvalidHeaders(String header) {

        Pattern returnPathPattern = Pattern.compile("^Return-Path: (.*)\\r?\\n", Pattern.MULTILINE);
        header = returnPathPattern.matcher(header).replaceAll("");

        Pattern senderPattern = Pattern.compile("^Sender: (.*)\\r?\\n", Pattern.MULTILINE);
        header = senderPattern.matcher(header).replaceAll("");

        Pattern messageIdPattern = Pattern.compile("^Message-ID: (.*)\\r?\\n", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        header = messageIdPattern.matcher(header).replaceAll("");

        Pattern dkimPattern = Pattern.compile("^DKIM-Signature: .*\\r?\\n(\\s+.*\\r?\\n)*", Pattern.MULTILINE);
        header = dkimPattern.matcher(header).replaceAll("");

        return header;
    }
}
