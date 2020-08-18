package io.harness.perpetualtask;

import static io.harness.network.SafeHttpCall.execute;

import com.google.inject.Inject;

import io.harness.delegate.task.azure.request.AzureVMSSListVMDataParameters;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.grpc.utils.AnyUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.AzureVmssInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;
import software.wings.beans.AzureConfig;
import software.wings.delegatetasks.azure.taskhandler.AzureVMSSSyncTaskHandler;
import software.wings.service.intfc.security.EncryptionService;

import java.time.Instant;
import java.util.List;

@Slf4j
public class AzureVMSSInstanceSyncDelegateExecutor implements PerpetualTaskExecutor {
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private AzureVMSSSyncTaskHandler azureVMSSSyncTaskHandler;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    logger.info("Running the InstanceSync perpetual task executor for task id: {}", taskId);
    AzureVmssInstanceSyncPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), AzureVmssInstanceSyncPerpetualTaskParams.class);
    AzureConfig azureConfig = (AzureConfig) kryoSerializer.asObject(taskParams.getAzureConfig().toByteArray());
    AzureVMSSTaskExecutionResponse azureVMSSTaskExecutionResponse = executeSyncTask(taskParams, azureConfig);
    try {
      logger.info("Publish instance sync result to manager for VMSS id {} and perpetual task {}",
          taskParams.getVmssId(), taskId.getId());
      execute(delegateAgentManagerClient.publishInstanceSyncResult(
          taskId.getId(), azureConfig.getAccountId(), azureVMSSTaskExecutionResponse));
    } catch (Exception ex) {
      logger.error(
          "Failed to publish the instance sync collection result to manager for VMSS id {} and perpetual task {}",
          taskParams.getVmssId(), taskId.getId(), ex);
    }
    return getPerpetualTaskResponse(azureVMSSTaskExecutionResponse);
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }

  private PerpetualTaskResponse getPerpetualTaskResponse(AzureVMSSTaskExecutionResponse executionResponse) {
    PerpetualTaskState taskState;
    String message;
    if (CommandExecutionStatus.FAILURE == executionResponse.getCommandExecutionStatus()) {
      taskState = PerpetualTaskState.TASK_RUN_FAILED;
      message = executionResponse.getErrorMessage();
    } else {
      taskState = PerpetualTaskState.TASK_RUN_SUCCEEDED;
      message = PerpetualTaskState.TASK_RUN_SUCCEEDED.name();
    }
    return PerpetualTaskResponse.builder()
        .responseCode(Response.SC_OK)
        .perpetualTaskState(taskState)
        .responseMessage(message)
        .build();
  }

  private AzureVMSSTaskExecutionResponse executeSyncTask(
      AzureVmssInstanceSyncPerpetualTaskParams taskParams, AzureConfig azureConfig) {
    List<EncryptedDataDetail> encryptedDataDetails =
        (List<EncryptedDataDetail>) kryoSerializer.asObject(taskParams.getAzureEncryptedData().toByteArray());
    encryptionService.decrypt(azureConfig, encryptedDataDetails);
    AzureVMSSListVMDataParameters parameters = AzureVMSSListVMDataParameters.builder()
                                                   .subscriptionId(taskParams.getSubscriptionId())
                                                   .resourceGroupName(taskParams.getResourceGroupName())
                                                   .vmssId(taskParams.getVmssId())
                                                   .build();
    try {
      return azureVMSSSyncTaskHandler.executeTask(parameters, azureConfig);
    } catch (Exception ex) {
      logger.error("Failed to execute instance sync task for VMSS id {}", taskParams.getVmssId(), ex);
      return AzureVMSSTaskExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ex.getMessage())
          .build();
    }
  }
}