package com.security.ragshield.controller;

import com.security.ragshield.service.PineconeRagShieldService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final PineconeRagShieldService shieldService;
    private final VectorStore vectorStore;

    public SearchController(PineconeRagShieldService shieldService, VectorStore vectorStore) {
        this.shieldService = shieldService;
        this.vectorStore = vectorStore;
    }

    /**
     * Executes a secure similarity search.
     * Accessible via GET /api/search?query=your-query&topK=3
     * Requires HTTP Basic Authentication.
     */
    @GetMapping
    public List<Document> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK) {
        return shieldService.securedSearch(query, topK);
    }

    /**
     * Ingests a new document with a specified security level.
     * Accessible via POST /api/search/ingest?content=text&securityLevel=ROLE_USER
     * Requires HTTP Basic Authentication.
     */
    @PostMapping("/ingest")
    public String ingest(
            @RequestParam String content,
            @RequestParam String securityLevel) {
        Document doc = new Document(content, Map.of("security_level", securityLevel));
        vectorStore.add(List.of(doc));
        return "Successfully ingested document with security level: " + securityLevel;
    }
}
