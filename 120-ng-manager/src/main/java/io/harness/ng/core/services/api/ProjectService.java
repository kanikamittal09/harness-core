package io.harness.ng.core.services.api;

import io.harness.ng.core.entities.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public interface ProjectService {
  Project create(Project project);

  Optional<Project> get(String projectId);

  Project update(Project project);

  Page<Project> list(String organizationId, Criteria criteria, Pageable pageable);

  Page<Project> list(Criteria criteria, Pageable pageable);

  boolean delete(String projectId);
}
