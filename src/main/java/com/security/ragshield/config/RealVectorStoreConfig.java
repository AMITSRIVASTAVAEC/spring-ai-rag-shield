package com.security.ragshield.config;

import org.springframework.ai.autoconfigure.vectorstore.pinecone.PineconeVectorStoreAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(name = "ragshield.mock", havingValue = "false")
@Import(PineconeVectorStoreAutoConfiguration.class)
public class RealVectorStoreConfig {
}
