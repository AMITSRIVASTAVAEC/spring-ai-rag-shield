package com.security.ragshield.service;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class EnterpriseRedactorTest {

    private static EnterpriseRedactor redactor;

    @BeforeAll
    static void setUp() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        props.setProperty("ner.useSUTime", "false");
        props.setProperty("ner.applyNumericClassifiers", "false");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        redactor = new EnterpriseRedactor(pipeline);
    }

    @Test
    void testRedactPersonEmailAndPhone() {
        String originalText = "My name is John Doe, my email is john.doe@example.com and phone is +1-555-555-5555.";
        String redactedText = redactor.redact(originalText);

        System.out.println("Original: " + originalText);
        System.out.println("Redacted: " + redactedText);

        assertTrue(redactedText.contains("[PERSON]"), "Text should contain [PERSON] tag");
        assertTrue(redactedText.contains("[EMAIL]"), "Text should contain [EMAIL] tag");
        assertTrue(redactedText.contains("[PHONE]"), "Text should contain [PHONE] tag");
        assertFalse(redactedText.contains("John Doe"), "Text should not contain original name");
        assertFalse(redactedText.contains("john.doe@example.com"), "Text should not contain original email");
        assertFalse(redactedText.contains("555-555-5555"), "Text should not contain original phone");
    }

    @Test
    void testMultipleEntities() {
        String text = "Contact Alice and Bob. Reach Alice at alice@mail.com and Bob at +359-888-123-456.";
        String redacted = redactor.redact(text);

        System.out.println("Multiple Redacted: " + redacted);

        assertTrue(redacted.contains("[PERSON]"));
        assertTrue(redacted.contains("[EMAIL]"));
        assertTrue(redacted.contains("[PHONE]"));
        assertFalse(redacted.contains("Alice"));
        assertFalse(redacted.contains("Bob"));
    }

    @Test
    void testRedactSsn() {
        String text = "Customer SSN is 123-45-6789. Contact customer service.";
        String redacted = redactor.redact(text);

        System.out.println("SSN Redacted: " + redacted);

        assertTrue(redacted.contains("[SSN]"));
        assertFalse(redacted.contains("123-45-6789"));
    }
}
