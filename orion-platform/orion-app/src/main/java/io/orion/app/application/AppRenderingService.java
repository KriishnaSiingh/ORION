package io.orion.app.application;

import io.orion.app.domain.AppDefinition;
import io.orion.app.domain.ViewDefinition;
import io.orion.knowledge.application.KnowledgeService;
import io.orion.knowledge.domain.KnowledgeObject;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppRenderingService {
    private final KnowledgeService knowledgeService;

    public AppRenderingService(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    public List<KnowledgeObject> resolveView(AppDefinition app, String viewId) {
        ViewDefinition view = app.getViews().stream()
                .filter(v -> v.getViewId().equals(viewId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("View not found: " + viewId));

        // For demo, we just return the first 20 objects of that type
        // In a real app, this would resolve filters and lauyouts
        return knowledgeService.search(null, view.getSourceObjectType(), 0, 20).items();
    }
}
