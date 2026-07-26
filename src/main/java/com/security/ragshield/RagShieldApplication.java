package com.security.ragshield;

import org.springframework.ai.autoconfigure.vectorstore.pinecone.PineconeVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = { PineconeVectorStoreAutoConfiguration.class })
public class RagShieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagShieldApplication.class, args);
    }
}
