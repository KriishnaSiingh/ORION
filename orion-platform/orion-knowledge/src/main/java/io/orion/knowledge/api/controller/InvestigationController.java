package io.orion.knowledge.api.controller;

import io.orion.knowledge.infrastructure.persistence.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/investigations")
public class InvestigationController {
    private final InvestigationBoardRepository repository;

    @Autowired
    public InvestigationController(InvestigationBoardRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<InvestigationBoard> createBoard(@RequestBody InvestigationBoard board) {
        board.setId(UUID.randomUUID().toString());
        board.setCreatedAt(Instant.now());
        board.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(repository.save(board));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvestigationBoard> getBoard(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<InvestigationBoard> updateBoard(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        return repository.findById(id).map(board -> {
            if (updates.containsKey("status")) {
                board.setStatus(InvestigationBoard.BoardStatus.valueOf((String) updates.get("status")));
            }
            if (updates.containsKey("findings")) {
                board.setFindings((List<String>) updates.get("findings"));
            }
            board.setUpdatedAt(Instant.now());
            return ResponseEntity.ok(repository.save(board));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<InvestigationBoard>> listBoards(@RequestParam String tenantId) {
        return ResponseEntity.ok(repository.findByTenantId(tenantId));
    }
}
