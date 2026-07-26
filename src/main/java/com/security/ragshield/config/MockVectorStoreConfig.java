package com.security.ragshield.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
@ConditionalOnProperty(name = "ragshield.mock", havingValue = "true", matchIfMissing = true)
public class MockVectorStoreConfig {

    @Bean
    public VectorStore vectorStore() {
        return new VectorStore() {
            private final List<Document> store = new ArrayList<>();

            {
                // Pre-populate with sample documents tagged with various security_level metadata
                store.add(new Document("doc-1", "Public Info: The office is located in Seattle. Contact hr@company.com.", Map.of("security_level", "ROLE_USER")));
                store.add(new Document("doc-2", "Manager confidential: Alice Smith has salary of $120,000. Phone is 555-123-4567.", Map.of("security_level", "ROLE_MANAGER")));
                store.add(new Document("doc-3", "Admin eyes only: Project Phoenix details. Chief architect is Bob Jones (bob.jones@phoenix.io).", Map.of("security_level", "ROLE_ADMIN")));
            }

            @Override
            public void add(List<Document> documents) {
                store.addAll(documents);
            }

            @Override
            public Optional<Boolean> delete(List<String> idList) {
                return Optional.of(store.removeIf(doc -> idList.contains(doc.getId())));
            }

            @Override
            public List<Document> similaritySearch(String query) {
                return store;
            }

            @Override
            public List<Document> similaritySearch(SearchRequest request) {
                Filter.Expression expression = request.getFilterExpression();
                if (expression == null) {
                    return store;
                }

                // Simulate metadata filtering (security_level == 'ROLE' or security_level IN ('ROLE_A', 'ROLE_B'))
                String exprString = expression.toString();
                List<Document> filtered = new ArrayList<>();
                for (Document doc : store) {
                    String docSecLevel = (String) doc.getMetadata().get("security_level");
                    if (docSecLevel != null && exprString.contains(docSecLevel)) {
                        filtered.add(doc);
                    }
                }
                return filtered;
            }
        };
    }
}
