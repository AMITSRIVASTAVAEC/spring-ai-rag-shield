package com.security.ragshield.service;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.Pair;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EnterpriseRedactor {

    private final StanfordCoreNLP pipeline;

    // Email regex matching standard RFC 5322 style email addresses
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    );

    // Phone regex matching common national and international phone number layouts
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{3,4}\\b"
    );

    // SSN regex matching standard US Social Security Numbers (XXX-XX-XXXX)
    private static final Pattern SSN_PATTERN = Pattern.compile(
            "\\b\\d{3}-\\d{2}-\\d{4}\\b"
    );

    public EnterpriseRedactor(StanfordCoreNLP pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * Redacts PERSON names, EMAIL addresses, and PHONE numbers from the input text.
     *
     * @param text The input text to process.
     * @return The redacted text.
     */
    public String redact(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        List<RedactionRange> ranges = new ArrayList<>();

        // 1. Detect PERSON tokens using Stanford CoreNLP
        CoreDocument doc = new CoreDocument(text);
        pipeline.annotate(doc);
        for (CoreEntityMention mention : doc.entityMentions()) {
            if ("PERSON".equalsIgnoreCase(mention.entityType())) {
                Pair<Integer, Integer> offsets = mention.charOffsets();
                ranges.add(new RedactionRange(offsets.first, offsets.second, "[PERSON]"));
            }
        }

        // 2. Detect EMAIL tokens using Regex
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        while (emailMatcher.find()) {
            ranges.add(new RedactionRange(emailMatcher.start(), emailMatcher.end(), "[EMAIL]"));
        }

        // 3. Detect PHONE tokens using Regex
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) {
            ranges.add(new RedactionRange(phoneMatcher.start(), phoneMatcher.end(), "[PHONE]"));
        }

        // 4. Detect SSN tokens using Regex
        Matcher ssnMatcher = SSN_PATTERN.matcher(text);
        while (ssnMatcher.find()) {
            ranges.add(new RedactionRange(ssnMatcher.start(), ssnMatcher.end(), "[SSN]"));
        }

        // 4. Sort and resolve overlaps, then apply redactions from end to beginning
        return applyRedactions(text, ranges);
    }

    private String applyRedactions(String text, List<RedactionRange> ranges) {
        if (ranges.isEmpty()) {
            return text;
        }

        // Sort by start position ascending, and end descending
        ranges.sort(Comparator.comparingInt((RedactionRange r) -> r.start)
                .thenComparing((RedactionRange r1, RedactionRange r2) -> Integer.compare(r2.end, r1.end)));

        List<RedactionRange> nonOverlapping = new ArrayList<>();
        int lastEnd = -1;
        for (RedactionRange range : ranges) {
            if (range.start >= lastEnd) {
                nonOverlapping.add(range);
                lastEnd = range.end;
            }
        }

        // Sort non-overlapping ranges descending by start index to prevent offset shifting issues
        nonOverlapping.sort(Comparator.comparingInt((RedactionRange r) -> r.start).reversed());

        StringBuilder sb = new StringBuilder(text);
        for (RedactionRange range : nonOverlapping) {
            sb.replace(range.start, range.end, range.label);
        }

        return sb.toString();
    }

    private static class RedactionRange {
        int start;
        int end;
        String label;

        RedactionRange(int start, int end, String label) {
            this.start = start;
            this.end = end;
            this.label = label;
        }
    }
}
