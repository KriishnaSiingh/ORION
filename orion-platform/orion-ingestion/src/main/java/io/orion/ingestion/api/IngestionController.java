package io.orion.ingestion.api;

import io.orion.ingestion.application.IngestionService;
import io.orion.ingestion.infrastructure.RawZoneService;
import io.orion.ingestion.domain.Pipeline;
import io.orion.ingestion.infrastructure.persistence.PipelineRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {
    private final RawZoneService rawZoneService;
    private final IngestionService ingestionService;
    private final PipelineRepository pipelineRepository;

    @Autowired
    public IngestionController(RawZoneService rawZoneService, IngestionService ingestionService, PipelineRepository pipelineRepository) {
        this.rawZoneService = rawZoneService;
        this.ingestionService = ingestionService;
        this.pipelineRepository = pipelineRepository;
    }

    @GetMapping("/pipelines")
    public ResponseEntity<List<Pipeline>> listPipelines(@RequestHeader(value = "X-Tenant-ID", required = false) String tenantId) {
        if (tenantId == null) {
            return ResponseEntity.ok(pipelineRepository.findAll());
        }
        return ResponseEntity.ok(pipelineRepository.findByTenantId(tenantId));
    }

    @PostMapping("/pipelines")
    public ResponseEntity<Pipeline> savePipeline(@RequestBody Pipeline pipeline) {
        if (pipeline.getPipelineId() == null) {
            pipeline.setPipelineId(UUID.randomUUID().toString());
        }
        return ResponseEntity.ok(pipelineRepository.save(pipeline));
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        String fileName = rawZoneService.storeFile(file);

        // In a real impl, we'd create an IngestionJob record in a DB
        // For MVP, we return the fileName as the jobId
        List<String> columns = ingestionService.detectColumns(fileName);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", fileName);
        response.put("columns", columns);
        response.put("fileName", file.getOriginalFilename());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> process(@RequestBody Map<String, Object> request) {
        String jobId = (String) request.get("jobId");
        String objectType = (String) request.get("objectType");
        Map<String, String> mappings = (Map<String, String>) request.get("mappings");

        var summary = ingestionService.processIngestion(jobId, objectType, mappings);

        Map<String, Object> response = new HashMap<>();
        response.put("summary", summary);

        return ResponseEntity.ok(response);
    }
}
