package io.orion.knowledge.infrastructure.vector;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.logging.Logger;

@Service
public class VectorService {
    private static final Logger logger = Logger.getLogger(VectorService.class.getName());

    /**
     * Mock implementation of embedding generation.
     * In a real system, this would call OpenAI's /v1/embeddings or a local model.
     */
    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[1536]; // Default dimension for OpenAI ada-002
        }

        logger.info("Generating embedding for text: " + text);

        // For demo purposes, we generate a deterministic pseudo-random vector
        float[] embedding = new float[1536];
        Random random = new Random(text.hashCode());
        for (int i = 0; i < 1536; i++) {
            embedding[i] = random.nextFloat();
        }
        return embedding;
    }
}
