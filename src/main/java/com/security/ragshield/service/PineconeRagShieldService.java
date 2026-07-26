package com.security.ragshield.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PineconeRagShieldService {

    private final VectorStore vectorStore;
    private final EnterpriseRedactor redactor;

    public PineconeRagShieldService(VectorStore vectorStore, EnterpriseRedactor redactor) {
        this.vectorStore = vectorStore;
        this.redactor = redactor;
    }

    /**
     * Performs a secured similarity search on Pinecone.
     * The method intercepts the request, builds a metadata filter based on user roles,
     * performs search, and redacts PII before returning the context.
     *
     * @param query The search query.
     * @param topK  Number of results to retrieve.
     * @return List of redacted documents that match the user's security level.
     */
    public List<Document> securedSearch(String query, int topK) {
        // 1. Extract authorities/roles from Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<String> roles = new ArrayList<>();
        if (authentication != null && authentication.isAuthenticated()) {
            roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
        }

        // 2. Construct metadata filters (security_level == 'ROLE')
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        Filter.Expression filterExpression;
        if (!roles.isEmpty()) {
            if (roles.size() == 1) {
                // If the user has a single role, filter security_level == ROLE
                filterExpression = builder.eq("security_level", roles.get(0)).build();
            } else {
                // If the user has multiple roles, construct an IN filter
                filterExpression = builder.in("security_level", roles).build();
            }
        } else {
            // Default fallback if unauthenticated (retrieve only guest-level content)
            filterExpression = builder.eq("security_level", "ROLE_GUEST").build();
        }

        // 3. Build search request containing query, top-k limit, and metadata filter expression
        SearchRequest searchRequest = SearchRequest.query(query)
                .withTopK(topK)
                .withFilterExpression(filterExpression);

        // 4. Perform vector similarity search
        List<Document> retrievedDocs = vectorStore.similaritySearch(searchRequest);

        // 5. Redact PII (PERSON, EMAIL, PHONE) in each retrieved document
        List<Document> redactedDocs = new ArrayList<>();
        for (Document doc : retrievedDocs) {
            String originalText = doc.getContent();
            String redactedText = redactor.redact(originalText);

            // Copy metadata and add redactor processing audit info
            Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
            metadata.put("pii_redacted", true);

            // Create new redacted document using the original document's ID and modified metadata
            Document redactedDoc = new Document(doc.getId(), redactedText, metadata);
            redactedDocs.add(redactedDoc);
        }

        return redactedDocs;
    }
}
