package io.orion.knowledge.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "investigation_boards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestigationBoard {
    @Id
    private String id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private BoardStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(name = "board_findings", joinColumns = @JoinColumn(name = "board_id"))
    @Column(name = "finding")
    private List<String> findings = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "board_objects", joinColumns = @JoinColumn(name = "board_id"))
    @Column(name = "object_id")
    private List<String> linkedObjectIds = new ArrayList<>();

    public enum BoardStatus {
        DRAFT, ACTIVE, COMPLETED, ARCHIVED
    }

    // Explicitly add setters to avoid Lombok annotation processing issues in some environments
    public void setId(String id) { this.id = id; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setStatus(BoardStatus status) { this.status = status; }
    public void setFindings(List<String> findings) { this.findings = findings; }
}
