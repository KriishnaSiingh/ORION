package io.orion.ingestion.infrastructure.persistence;

import io.orion.ingestion.domain.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PipelineRepository extends JpaRepository<Pipeline, String> {
    List<Pipeline> findByTenantId(String tenantId);
}
