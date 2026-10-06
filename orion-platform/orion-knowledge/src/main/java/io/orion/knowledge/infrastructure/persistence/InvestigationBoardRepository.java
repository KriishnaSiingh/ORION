package io.orion.knowledge.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InvestigationBoardRepository extends JpaRepository<InvestigationBoard, String> {
    java.util.List<InvestigationBoard> findByTenantId(String tenantId);
}
