package io.orion.app.web.controller;

import io.orion.app.application.AppRenderingService;
import io.orion.app.domain.AppDefinition;
import io.orion.app.domain.ViewDefinition;
import io.orion.app.infrastructure.persistence.AppDefinitionRepository;
import io.orion.knowledge.domain.KnowledgeObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/apps")
public class AppController {
    private final AppRenderingService renderingService;
    private final AppDefinitionRepository repository;

    @Autowired
    public AppController(AppRenderingService renderingService, AppDefinitionRepository repository) {
        this.renderingService = renderingService;
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<AppDefinition>> listApps(@RequestHeader(value = "X-Tenant-ID", required = false) String tenantId) {
        // For now, we return all apps since AppDefinition doesn't have a tenantId yet
        // In a full implementation, we'd filter by tenantId
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<AppDefinition> saveApp(@RequestBody AppDefinition app) {
        if (app.getAppId() == null) {
            app.setAppId(UUID.randomUUID().toString());
        }
        return ResponseEntity.ok(repository.save(app));
    }

    @PutMapping("/{appId}/publish")
    public ResponseEntity<Void> publishApp(@PathVariable String appId) {
        // In a real system, this would change a status flag to 'PUBLISHED'
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{appId}/views/{viewId}")
    public List<KnowledgeObject> renderView(@PathVariable String appId, @PathVariable String viewId) {
        AppDefinition app = repository.findById(appId)
                .orElseThrow(() -> new RuntimeException("App not found"));

        return renderingService.resolveView(app, viewId);
    }
}
