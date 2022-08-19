/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.plan.execution.service;

import static io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.concurrency.ConcurrentChildInstance;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.utils.OrchestrationUtils;
import io.harness.execution.NodeExecution;
import io.harness.graph.stepDetail.service.PmsGraphStepDetailsService;
import io.harness.plan.NodeType;
import io.harness.plancreator.strategy.StrategyType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.plan.execution.ExecutionSummaryUpdateUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.GraphLayoutNodeDTO;
import io.harness.pms.plan.execution.beans.dto.GraphLayoutNodeDTO.GraphLayoutNodeDTOKeys;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class PmsExecutionSummaryServiceImpl implements PmsExecutionSummaryService {
  @Inject NodeExecutionService nodeExecutionService;
  @Inject private PmsExecutionSummaryRespository pmsExecutionSummaryRepository;
  @Inject private PmsGraphStepDetailsService pmsGraphStepDetailsService;

  /**
   * Updates all the fields in the stage graph from nodeExecutions.
   * @param planExecutionId
   */
  public void updateStageOfIdentityType(String planExecutionId, Update update) {
    List<NodeExecution> nodeExecutions =
        nodeExecutionService.fetchStageExecutionsWithEndTsAndStatusProjection(planExecutionId);
    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity = null;

    // This is done inorder to reduce the load while updating stageInfo. Here we will update only the status.
    // Todo: Refactor this piece of code
    for (NodeExecution nodeExecution : nodeExecutions) {
      if (nodeExecution.getStepType().getStepCategory() != StepCategory.STRATEGY) {
        String graphNodeId = AmbianceUtils.obtainCurrentSetupId(nodeExecution.getAmbiance());
        if (AmbianceUtils.getStrategyLevelFromAmbiance(nodeExecution.getAmbiance()).isPresent()) {
          graphNodeId = nodeExecution.getUuid();
        }
        update.set(PlanExecutionSummaryKeys.layoutNodeMap + "." + graphNodeId + ".status",
            ExecutionStatus.getExecutionStatus(nodeExecution.getStatus()));
        update.set(PlanExecutionSummaryKeys.layoutNodeMap + "." + graphNodeId + ".endTs", nodeExecution.getEndTs());
      } else {
        if (!AmbianceUtils.isCurrentStrategyLevelAtStage(nodeExecution.getAmbiance())) {
          continue;
        }
        if (pipelineExecutionSummaryEntity == null) {
          Optional<PipelineExecutionSummaryEntity> entity =
              getPipelineExecutionSummary(AmbianceUtils.getAccountId(nodeExecution.getAmbiance()),
                  AmbianceUtils.getOrgIdentifier(nodeExecution.getAmbiance()),
                  AmbianceUtils.getProjectIdentifier(nodeExecution.getAmbiance()), planExecutionId);
          if (!entity.isPresent()) {
            continue;
          }
          pipelineExecutionSummaryEntity = entity.get();
        }

        Map<String, GraphLayoutNodeDTO> graphLayoutNode = pipelineExecutionSummaryEntity.getLayoutNodeMap();

        if (graphLayoutNode.get(nodeExecution.getNodeId()) == null) {
          log.error("layout node is null for key [{}] in GraphLayoutNodeMap for planExecutionId: [{}]",
              nodeExecution.getNodeId(), planExecutionId);
          continue;
        }

        List<NodeExecution> childrenNodeExecution = nodeExecutions.stream()
                                                        .filter(o -> o.getParentId().equals(nodeExecution.getUuid()))
                                                        .collect(Collectors.toList());
        if (!pipelineExecutionSummaryEntity.getLayoutNodeMap()
                 .get(nodeExecution.getNodeId())
                 .getNodeType()
                 .equals(StrategyType.PARALLELISM.name())) {
          updateStrategyNodeFields(nodeExecution, update, true);
        }

        String stageSetupId = getStageSetupId(childrenNodeExecution, graphLayoutNode, nodeExecution);

        for (NodeExecution childNodeExecution : childrenNodeExecution) {
          if (!graphLayoutNode.get(nodeExecution.getNodeId())
                   .getEdgeLayoutList()
                   .getCurrentNodeChildren()
                   .contains(childNodeExecution.getUuid())) {
            addStageIdentityNodeInGraphIfUnderStrategy(planExecutionId, childNodeExecution, update,
                graphLayoutNode.get(stageSetupId), nodeExecution.getNodeId(), stageSetupId);
          }
        }
      }
    }
  }

  @Override
  public void regenerateStageLayoutGraph(String planExecutionId, List<NodeExecution> nodeExecutions) {
    Update update = new Update();
    for (NodeExecution nodeExecution : nodeExecutions) {
      addStageNodeInGraphIfUnderStrategy(planExecutionId, nodeExecution, update);
      ExecutionSummaryUpdateUtils.addStageUpdateCriteria(update, nodeExecution);
    }
    Criteria criteria = Criteria.where(PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }

  @Override
  public void updateEndTs(String planExecutionId, NodeExecution nodeExecution) {
    Update update = new Update();
    boolean updated = false;

    // Update endTs at pipeline level
    if (OrchestrationUtils.isPipelineNode(nodeExecution)) {
      if (nodeExecution.getEndTs() != null) {
        updated = true;
        update.set(PlanExecutionSummaryKeys.endTs, nodeExecution.getEndTs());
      }
    }

    // Update endTs at stage level
    if (OrchestrationUtils.isStageNode(nodeExecution)) {
      String stageUuid = AmbianceUtils.obtainCurrentSetupId(nodeExecution.getAmbiance());
      if (nodeExecution.getEndTs() != null) {
        updated = true;
        update.set(PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".endTs", nodeExecution.getEndTs());
      }
    }
    if (updated) {
      Criteria criteria = Criteria.where(PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
      Query query = new Query(criteria);
      pmsExecutionSummaryRepository.update(query, update);
    }
  }

  @Override
  public void addStageNodeInGraphIfUnderStrategy(
      String planExecutionId, NodeExecution nodeExecution, Update summaryUpdate) {
    if (OrchestrationUtils.isStageNode(nodeExecution)
        && AmbianceUtils.getStrategyLevelFromAmbiance(nodeExecution.getAmbiance()).isPresent()
        && nodeExecution.getNode().getNodeType() != NodeType.IDENTITY_PLAN_NODE) {
      String stageSetupId = nodeExecution.getNodeId();
      Update update = new Update();
      Ambiance ambiance = nodeExecution.getAmbiance();
      Optional<PipelineExecutionSummaryEntity> entity =
          getPipelineExecutionSummary(AmbianceUtils.getAccountId(ambiance), AmbianceUtils.getOrgIdentifier(ambiance),
              AmbianceUtils.getProjectIdentifier(ambiance), planExecutionId);
      if (!entity.isPresent()) {
        return;
      }
      PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity = entity.get();
      Map<String, GraphLayoutNodeDTO> graphLayoutNodeDTOMap = pipelineExecutionSummaryEntity.getLayoutNodeMap();
      if (graphLayoutNodeDTOMap.containsKey(nodeExecution.getUuid())
          && graphLayoutNodeDTOMap.get(nodeExecution.getUuid()).getNodeExecutionId() != null) {
        return;
      }
      GraphLayoutNodeDTO graphLayoutNodeDTO = graphLayoutNodeDTOMap.get(stageSetupId);
      cloneGraphLayoutNodeDtoWithModifications(graphLayoutNodeDTO, nodeExecution, update);
      String strategyNodeId = AmbianceUtils.getStrategyLevelFromAmbiance(ambiance).get().getSetupId();
      update.addToSet(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + strategyNodeId
              + ".edgeLayoutList.currentNodeChildren",
          nodeExecution.getUuid());
      summaryUpdate.pull(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + strategyNodeId
              + ".edgeLayoutList.currentNodeChildren",
          stageSetupId);
      summaryUpdate.set(
          PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageSetupId + ".hidden", true);
      update(planExecutionId, update);
    }
  }

  @Override
  public void updateStrategyNode(String planExecutionId, NodeExecution nodeExecution, Update update) {
    if (nodeExecution.getStepType().getStepCategory() == StepCategory.STRATEGY
        && AmbianceUtils.isCurrentStrategyLevelAtStage(nodeExecution.getAmbiance())
        && nodeExecution.getNode().getNodeType() != NodeType.IDENTITY_PLAN_NODE) {
      updateStrategyNodeFields(nodeExecution, update, false);
    }
  }

  private void updateStrategyNodeFields(
      NodeExecution nodeExecution, Update update, boolean isStrategyTypeAlreadyChecked) {
    ConcurrentChildInstance concurrentChildInstance =
        pmsGraphStepDetailsService.fetchConcurrentChildInstance(nodeExecution.getUuid());
    if (concurrentChildInstance != null) {
      if (!nodeExecution.getExecutableResponses().isEmpty()) {
        if (isStrategyTypeAlreadyChecked
            || nodeExecution.getNode().getStepParameters().containsKey("strategyType")
                && !nodeExecution.getNode()
                        .getStepParameters()
                        .get("strategyType")
                        .equals(StrategyType.PARALLELISM.name())) {
          update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "."
                  + nodeExecution.getNodeId() + ".moduleInfo.maxConcurrency.value",
              nodeExecution.getExecutableResponses().get(0).getChildren().getMaxConcurrency());
        }
      }
    }
  }

  @Override
  public Optional<PipelineExecutionSummaryEntity> getPipelineExecutionSummary(
      String accountId, String orgId, String projectId, String planExecutionId) {
    return pmsExecutionSummaryRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPlanExecutionId(
        accountId, orgId, projectId, planExecutionId);
  }

  @Override
  public void update(String planExecutionId, Update update) {
    Criteria criteria = Criteria.where(PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }

  public void addStageIdentityNodeInGraphIfUnderStrategy(String planExecutionId, NodeExecution nodeExecution,
      Update summaryUpdate, GraphLayoutNodeDTO graphLayoutNodeDTO, String strategyNodeId, String stageSetupId) {
    if (AmbianceUtils.getStrategyLevelFromAmbiance(nodeExecution.getAmbiance()).isPresent()) {
      Update update = new Update();

      cloneGraphLayoutNodeDtoWithModifications(graphLayoutNodeDTO, nodeExecution, update);
      update.addToSet(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + strategyNodeId
              + ".edgeLayoutList.currentNodeChildren",
          nodeExecution.getUuid());
      summaryUpdate.pull(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + strategyNodeId
              + ".edgeLayoutList.currentNodeChildren",
          stageSetupId);
      summaryUpdate.set(
          PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageSetupId + ".hidden", true);
      // fire only one update.
      update(planExecutionId, update);
    }
  }

  /**
   * Modifies the identifier and name of the dummy node we are copying.
   * @param graphLayoutNodeDTO
   * @param nodeExecution
   */
  private void cloneGraphLayoutNodeDtoWithModifications(
      GraphLayoutNodeDTO graphLayoutNodeDTO, NodeExecution nodeExecution, Update update) {
    String baseKey =
        PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + nodeExecution.getUuid() + ".";
    update.set(baseKey + GraphLayoutNodeDTOKeys.nodeType, graphLayoutNodeDTO.getNodeType());
    update.set(baseKey + GraphLayoutNodeDTOKeys.nodeGroup, graphLayoutNodeDTO.getNodeGroup());
    update.set(baseKey + GraphLayoutNodeDTOKeys.module, graphLayoutNodeDTO.getModule());
    update.set(baseKey + GraphLayoutNodeDTOKeys.edgeLayoutList, graphLayoutNodeDTO.getEdgeLayoutList());
    update.set(baseKey + GraphLayoutNodeDTOKeys.skipInfo, graphLayoutNodeDTO.getSkipInfo());
    update.set(baseKey + GraphLayoutNodeDTOKeys.nodeRunInfo, graphLayoutNodeDTO.getNodeRunInfo());
    update.set(baseKey + GraphLayoutNodeDTOKeys.nodeIdentifier, nodeExecution.getIdentifier());
    update.set(baseKey + GraphLayoutNodeDTOKeys.name, nodeExecution.getName());
    update.set(baseKey + GraphLayoutNodeDTOKeys.nodeUuid, nodeExecution.getNodeId());
  }

  private String getStageSetupId(List<NodeExecution> childrenNodeExecution,
      Map<String, GraphLayoutNodeDTO> graphLayoutNode, NodeExecution nodeExecution) {
    String stageSetupId = null;
    // Get the stageSetupId from ChildNodeExecutions of strategy. Will be null if all the stages were successful in
    // previous execution.
    for (NodeExecution childNodeExecution : childrenNodeExecution) {
      if (childNodeExecution.getNodeType() == NodeType.PLAN_NODE && stageSetupId == null) {
        stageSetupId = childNodeExecution.getNodeId();
      }
    }

    if (stageSetupId == null) {
      stageSetupId = graphLayoutNode.get(nodeExecution.getNodeId()).getEdgeLayoutList().getCurrentNodeChildren().get(0);
    }

    return stageSetupId;
  }
}
