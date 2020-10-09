package io.harness.cdng.pipeline.service;

import io.harness.cdng.pipeline.beans.dto.CDPipelineSummaryResponseDTO;
import io.harness.cdng.pipeline.beans.dto.NGPipelineResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public interface PipelineService {
  String createPipeline(String yaml, String accountId, String orgId, String projectId);
  String updatePipeline(String yaml, String accountId, String orgId, String projectId, String pipelineId);
  Optional<NGPipelineResponseDTO> getPipeline(String pipelineId, String accountId, String orgId, String projectId);
  Page<CDPipelineSummaryResponseDTO> getPipelines(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable, String searchTerm);
  boolean deletePipeline(String accountId, String orgId, String projectId, String pipelineId);
  Map<String, String> getPipelineIdentifierToName(
      String accountId, String orgId, String projectId, @NotNull List<String> pipelineIdentifiers);
}
