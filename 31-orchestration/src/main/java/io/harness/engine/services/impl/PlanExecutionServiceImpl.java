package io.harness.engine.services.impl;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.google.inject.Inject;

import io.harness.engine.services.PlanExecutionService;
import io.harness.engine.services.repositories.PlanExecutionRepository;
import io.harness.engine.status.StepStatusUpdateInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.execution.status.Status;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.EnumSet;
import java.util.function.Consumer;

@Slf4j
public class PlanExecutionServiceImpl implements PlanExecutionService {
  @Inject private PlanExecutionRepository planExecutionRepository;
  @Inject private MongoTemplate mongoTemplate;

  @Override
  public PlanExecution save(PlanExecution planExecution) {
    return planExecutionRepository.save(planExecution);
  }

  /**
   * Always use this method while updating statuses. This guarantees we a hopping from correct statuses.
   * As we don't have transactions it is possible that your execution state is manipulated by some other thread and
   * your transition is no longer valid.
   *
   * Like your workflow is aborted but some other thread try to set it to running. Same logic applied to plan execution
   * status as well
   */
  @Override
  public PlanExecution updateStatusWithOps(
      @NonNull String planExecutionId, @NonNull Status status, Consumer<Update> ops) {
    EnumSet<Status> allowedStartStatuses = Status.obtainAllowedStartSet(status);
    Query query = query(where(PlanExecutionKeys.uuid).is(planExecutionId))
                      .addCriteria(where(PlanExecutionKeys.status).in(allowedStartStatuses));
    Update updateOps = new Update().set(PlanExecutionKeys.status, status);
    if (ops != null) {
      ops.accept(updateOps);
    }
    PlanExecution updated = mongoTemplate.findAndModify(
        query, updateOps, new FindAndModifyOptions().upsert(false).returnNew(true), PlanExecution.class);
    if (updated == null) {
      logger.warn("Cannot update execution status for the node {} with {}", planExecutionId, status);
    }
    return updated;
  }

  @Override
  public PlanExecution updateStatus(@NonNull String planExecutionId, @NonNull Status status) {
    return updateStatusWithOps(planExecutionId, status, null);
  }

  @Override
  public PlanExecution update(@NonNull String planExecutionId, @NonNull Consumer<Update> ops) {
    Query query = query(where(PlanExecutionKeys.uuid).is(planExecutionId));
    Update updateOps = new Update();
    ops.accept(updateOps);
    PlanExecution updated = mongoTemplate.findAndModify(query, updateOps, PlanExecution.class);
    if (updated == null) {
      throw new InvalidRequestException("Node Execution Cannot be updated with provided operations" + planExecutionId);
    }
    return updated;
  }

  @Override
  public PlanExecution get(String planExecutionId) {
    return planExecutionRepository.findById(planExecutionId)
        .orElseThrow(() -> new InvalidRequestException("Plan Execution is null for id: " + planExecutionId));
  }

  @Override
  public void onStepStatusUpdate(StepStatusUpdateInfo stepStatusUpdateInfo) {
    logger.info("State Status Update Callback Fired : {}", stepStatusUpdateInfo);
  }
}
