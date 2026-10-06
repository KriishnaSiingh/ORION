package io.orion.ingestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "ingestion_pipelines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pipeline {
    @Id
    private String pipelineId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String targetObjectType;

    @ElementCollection
    @CollectionTable(name = "pipeline_mappings", joinColumns = @JoinColumn(name = "pipeline_id"))
    @MapKeyColumn(name = "csv_column")
    @Column(name = "property_name")
    private Map<String, String> mappings = new HashMap<>();

    private String tenantId;
    private Instant createdAt;
    private Instant updatedAt;

    // Explicit methods to avoid Lombok issues
    public String getPipelineId() { return pipelineId; }
    public void setPipelineId(String pipelineId) { this.pipelineId = pipelineId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTargetObjectType() { return targetObjectType; }
    public void setTargetObjectType(String targetObjectType) { this.targetObjectType = targetObjectType; }
    public Map<String, String> getMappings() { return mappings; }
    public void setMappings(Map<String, String> mappings) { this.mappings = mappings; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
