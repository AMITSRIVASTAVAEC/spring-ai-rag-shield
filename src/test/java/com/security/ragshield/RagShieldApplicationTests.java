package com.security.ragshield;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.vectorstore.VectorStore;

@SpringBootTest
class RagShieldApplicationTests {

    @MockBean
    private VectorStore vectorStore;

    @Test
    void contextLoads() {
        // Verifies that the Spring context loads successfully with Stanford CoreNLP and all middleware components.
    }
}
