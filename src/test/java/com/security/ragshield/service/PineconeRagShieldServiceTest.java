package com.security.ragshield.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PineconeRagShieldServiceTest {

    private VectorStore vectorStore;
    private EnterpriseRedactor redactor;
    private PineconeRagShieldService shieldService;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        redactor = mock(EnterpriseRedactor.class);
        shieldService = new PineconeRagShieldService(vectorStore, redactor);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSecuredSearchWithSingleRole() {
        // Arrange
        // Set authenticated user with ROLE_USER
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password",
                        AuthorityUtils.createAuthorityList("ROLE_USER"))
        );

        Document doc = new Document("doc-1", "Original PII context belonging to Alice", Map.of("security_level", "ROLE_USER"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));
        when(redactor.redact("Original PII context belonging to Alice")).thenReturn("Original PII context belonging to [PERSON]");

        // Act
        List<Document> results = shieldService.securedSearch("test query", 3);

        // Assert
        assertEquals(1, results.size());
        Document redactedDoc = results.get(0);
        assertEquals("doc-1", redactedDoc.getId());
        assertEquals("Original PII context belonging to [PERSON]", redactedDoc.getContent());
        assertEquals(true, redactedDoc.getMetadata().get("pii_redacted"));

        // Capture the search request and verify filter expression
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest capturedRequest = captor.getValue();
        assertEquals("test query", capturedRequest.getQuery());
        assertEquals(3, capturedRequest.getTopK());

        Filter.Expression filterExpression = capturedRequest.getFilterExpression();
        assertNotNull(filterExpression);
        // String representation of filter containing key and value
        String exprStr = filterExpression.toString();
        assertTrue(exprStr.contains("security_level"));
        assertTrue(exprStr.contains("ROLE_USER"));
    }

    @Test
    void testSecuredSearchWithMultipleRoles() {
        // Arrange
        // Set authenticated user with ROLE_USER and ROLE_MANAGER
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("manager", "password",
                        AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_MANAGER"))
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        // Act
        shieldService.securedSearch("another query", 5);

        // Assert
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest capturedRequest = captor.getValue();
        Filter.Expression filterExpression = capturedRequest.getFilterExpression();
        assertNotNull(filterExpression);
        // String representation of filter containing key, IN operator, and values
        String exprStr = filterExpression.toString();
        assertTrue(exprStr.contains("security_level"));
        assertTrue(exprStr.contains("ROLE_USER"));
        assertTrue(exprStr.contains("ROLE_MANAGER"));
    }

    @Test
    void testSecuredSearchUnauthenticatedDefaultsToGuest() {
        // Arrange
        // No security context set
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        // Act
        shieldService.securedSearch("guest query", 1);

        // Assert
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest capturedRequest = captor.getValue();
        Filter.Expression filterExpression = capturedRequest.getFilterExpression();
        assertNotNull(filterExpression);
        String exprStr = filterExpression.toString();
        assertTrue(exprStr.contains("security_level"));
        assertTrue(exprStr.contains("ROLE_GUEST"));
    }
}
