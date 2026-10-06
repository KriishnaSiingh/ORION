package io.orion.knowledge.api;

import io.orion.knowledge.application.KnowledgeService;
import io.orion.knowledge.application.service.HybridSearchService;
import io.orion.knowledge.domain.KnowledgeObject;
import io.orion.shared.paging.PageResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {
    private final KnowledgeService service;
    private final HybridSearchService hybridSearchService;

    public KnowledgeController(KnowledgeService service, HybridSearchService hybridSearchService) {
        this.service = service;
        this.hybridSearchService = hybridSearchService;
    }

    @GetMapping("/search")
    public PageResult<KnowledgeObject> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(q, type, page, size);
    }

    @GetMapping("/hybrid-search")
    public List<KnowledgeObject> hybridSearch(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int limit) {
        return hybridSearchService.hybridSearch(q, type, limit);
    }

    @GetMapping("/objects/{id}")
    public KnowledgeObject getObject(@PathVariable String id) {
        return service.getObject(id);
    }

    @PostMapping("/objects")
    public KnowledgeObject create(@RequestBody KnowledgeObject object) {
        return service.createObject(object);
    }

    @PostMapping("/links")
    public void createLink(@RequestBody Map<String, String> request) {
        String sourceId = request.get("sourceId");
        String targetId = request.get("targetId");
        String linkType = request.get("linkType");
        service.createLink(sourceId, targetId, linkType);
    }

    @GetMapping("/objects/{id}/links")
    public List<KnowledgeObject> getLinks(@PathVariable String id) {
        return service.getRelated(id);
    }
}
