/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.approval.custom;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.delegate.task.shell.ShellScriptTaskNG.COMMAND_UNIT;

import static software.wings.beans.TaskType.SHELL_SCRIPT_TASK_NG;

import static com.google.protobuf.Duration.newBuilder;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.TaskType;
import io.harness.delegate.task.shell.ShellScriptTaskParametersNG;
import io.harness.engine.pms.tasks.NgDelegate2TaskExecutor;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.iterator.PersistenceIterator;
import io.harness.logging.AutoLogContext;
import io.harness.logging.LogLevel;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.logstreaming.NGLogCallback;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.steps.approval.step.ApprovalInstanceService;
import io.harness.steps.approval.step.custom.CustomApprovalHelperService;
import io.harness.steps.approval.step.custom.entities.CustomApprovalInstance;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.entities.ApprovalInstance.ApprovalInstanceKeys;
import io.harness.steps.shellscript.ShellScriptHelperService;
import io.harness.steps.shellscript.ShellScriptStepParameters;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.beans.LogColor;
import software.wings.beans.LogHelper;
import software.wings.beans.LogWeight;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.time.Duration;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class CustomApprovalHelperServiceImpl implements CustomApprovalHelperService {
  private final NgDelegate2TaskExecutor ngDelegate2TaskExecutor;
  private final KryoSerializer kryoSerializer;
  private final WaitNotifyEngine waitNotifyEngine;
  private final LogStreamingStepClientFactory logStreamingStepClientFactory;
  private final String publisherName;
  private final PmsGitSyncHelper pmsGitSyncHelper;
  private final ShellScriptHelperService shellScriptHelperService;
  private final ApprovalInstanceService approvalInstanceService;

  @Inject
  public CustomApprovalHelperServiceImpl(NgDelegate2TaskExecutor ngDelegate2TaskExecutor, KryoSerializer kryoSerializer,
      WaitNotifyEngine waitNotifyEngine, LogStreamingStepClientFactory logStreamingStepClientFactory,
      @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName, PmsGitSyncHelper pmsGitSyncHelper,
      ShellScriptHelperService shellScriptHelperService, ApprovalInstanceService approvalInstanceService) {
    this.ngDelegate2TaskExecutor = ngDelegate2TaskExecutor;
    this.kryoSerializer = kryoSerializer;
    this.waitNotifyEngine = waitNotifyEngine;
    this.logStreamingStepClientFactory = logStreamingStepClientFactory;
    this.publisherName = publisherName;
    this.pmsGitSyncHelper = pmsGitSyncHelper;
    this.shellScriptHelperService = shellScriptHelperService;
    this.approvalInstanceService = approvalInstanceService;
  }

  @Override
  public void handlePollingEvent(PersistenceIterator<ApprovalInstance> iterator, CustomApprovalInstance instance) {
    try (PmsGitSyncBranchContextGuard ignore1 =
             pmsGitSyncHelper.createGitSyncBranchContextGuard(instance.getAmbiance(), true);
         AutoLogContext ignore2 = instance.autoLogContext()) {
      handlePollingEventInternal(iterator, instance);
    }
  }

  private void handlePollingEventInternal(
      PersistenceIterator<ApprovalInstance> iterator, CustomApprovalInstance instance) {
    Ambiance ambiance = instance.getAmbiance();
    NGLogCallback logCallback = new NGLogCallback(logStreamingStepClientFactory, ambiance, COMMAND_UNIT, false);

    try {
      log.info("Polling custom approval instance");
      logCallback.saveExecutionLog("-----");
      logCallback.saveExecutionLog(LogHelper.color(
          "Running custom shell script to check approval/rejection criteria", LogColor.White, LogWeight.Bold));

      String instanceId = instance.getId();
      String accountIdentifier = AmbianceUtils.getAccountId(ambiance);
      String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
      String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
      log.info(String.format("Creating parameters for CustomApproval Instance with id : %s", instanceId));

      validateField(instanceId, ApprovalInstanceKeys.id);
      validateField(accountIdentifier, "accountIdentifier");
      validateField(orgIdentifier, "orgIdentifier");
      validateField(projectIdentifier, "projectIdentifier");

      ShellScriptTaskParametersNG scriptTaskParametersNG = buildShellScriptTaskParametersNG(ambiance, instance);
      log.info("Queuing Custom Approval delegate task");
      String taskId = queueTask(ambiance, instance, scriptTaskParametersNG);
      log.info("Custom Approval Instance queued task with taskId - {}", taskId);
      logCallback.saveExecutionLog(String.format("Custom Shell Script Approval: %s", taskId));
    } catch (Exception ex) {
      logCallback.saveExecutionLog(
          String.format("Error creating task to run the custom shell script: %s", ExceptionUtils.getMessage(ex)),
          LogLevel.WARN);
      log.warn("Error creating task for running the shell script approval while polling", ex);
      resetNextIteration(iterator, instance);
    }
  }

  private ShellScriptTaskParametersNG buildShellScriptTaskParametersNG(
      @Nonnull Ambiance ambiance, @Nonnull CustomApprovalInstance customApprovalInstance) {
    ShellScriptStepParameters shellScriptStepParameters = customApprovalInstance.toShellScriptStepParameters();
    return shellScriptHelperService.buildShellScriptTaskParametersNG(ambiance, shellScriptStepParameters);
  }

  private String queueTask(Ambiance ambiance, CustomApprovalInstance approvalInstance,
      ShellScriptTaskParametersNG shellScriptTaskParametersNG) {
    TaskRequest taskRequest = prepareCustomApprovalTaskRequest(ambiance, approvalInstance, shellScriptTaskParametersNG);
    String taskId =
        ngDelegate2TaskExecutor.queueTask(ambiance.getSetupAbstractionsMap(), taskRequest, Duration.ofSeconds(0));
    NotifyCallback callback = CustomApprovalCallback.builder().approvalInstanceId(approvalInstance.getId()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, taskId);
    return taskId;
  }

  private TaskRequest prepareCustomApprovalTaskRequest(
      Ambiance ambiance, CustomApprovalInstance instance, ShellScriptTaskParametersNG stepParameters) {
    TaskDetails taskDetails =
        TaskDetails.newBuilder()
            .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(stepParameters) == null
                    ? new byte[] {}
                    : kryoSerializer.asDeflatedBytes(stepParameters)))
            .setExecutionTimeout(
                newBuilder().setSeconds(instance.getScriptTimeout().getValue().getTimeoutInMillis() / 1000).build())
            .setMode(TaskMode.ASYNC)
            .setParked(false)
            .setType(TaskType.newBuilder().setType(SHELL_SCRIPT_TASK_NG.name()).build())
            .build();

    List<TaskSelector> selectors = TaskSelectorYaml.toTaskSelector(instance.getDelegateSelectors());
    return StepUtils.prepareTaskRequest(ambiance, taskDetails, Lists.newArrayList(COMMAND_UNIT), selectors, null, true);
  }

  private void validateField(String name, String value) {
    if (isBlank(value)) {
      throw new InvalidRequestException(format("Field %s can't be empty", name));
    }
  }

  private void resetNextIteration(PersistenceIterator<ApprovalInstance> iterator, CustomApprovalInstance instance) {
    approvalInstanceService.resetNextIterations(instance.getId(), instance.recalculateNextIterations());
    if (iterator != null) {
      iterator.wakeup();
    }
  }
}
