package com.security.ragshield.config;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class CoreNlpConfig {

    @Bean
    public StanfordCoreNLP stanfordCoreNLP() {
        Properties props = new Properties();
        // Setup annotators for Named Entity Recognition (NER)
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        // Disable unnecessary heavier annotators to optimize startup time
        props.setProperty("ner.useSUTime", "false");
        props.setProperty("ner.applyNumericClassifiers", "false");
        return new StanfordCoreNLP(props);
    }
}
