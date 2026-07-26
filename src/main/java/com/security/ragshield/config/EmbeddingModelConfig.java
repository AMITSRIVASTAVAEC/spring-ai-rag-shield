package com.security.ragshield.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;

@Configuration
public class EmbeddingModelConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = new ArrayList<>();
                int idx = 0;
                for (String text : request.getInstructions()) {
                    List<Double> vector = DoubleStream.generate(() -> 0.1)
                            .limit(1024)
                            .boxed()
                            .toList();
                    embeddings.add(new Embedding(vector, idx++));
                }
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public List<Double> embed(String text) {
                return DoubleStream.generate(() -> 0.1)
                        .limit(1024)
                        .boxed()
                        .toList();
            }

            @Override
            public List<Double> embed(Document document) {
                return embed(document.getContent());
            }

            @Override
            public int dimensions() {
                return 1024;
            }
        };
    }
}
