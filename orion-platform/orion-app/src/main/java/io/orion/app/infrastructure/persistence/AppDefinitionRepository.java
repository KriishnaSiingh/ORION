package io.orion.app.infrastructure.persistence;

import io.orion.app.domain.AppDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppDefinitionRepository extends JpaRepository<AppDefinition, String> {
}
