package io.harness.redesign.states.http.chain;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.chain.task.TaskChainExecutable;
import io.harness.facilitator.modes.chain.task.TaskChainResponse;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.tasks.Cd1SetupFields;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.TaskType;

import java.util.Map;

@Redesign
@ExcludeRedesign
@OwnedBy(HarnessTeam.CDC)
public class BasicHttpChainStep implements Step, TaskChainExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("HTTP_CHAIN").build();
  private static final int socketTimeoutMillis = 10000;

  @Override
  public TaskChainResponse startChainLink(
      Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage) {
    BasicHttpChainStepParameters parameters =
        obtainBasicHttpChainStepParameters((BasicHttpChainStepParameters) stepParameters);
    BasicHttpStepParameters linkParam = parameters.getLinkParameters().get(0);
    DelegateTask task = buildTask(ambiance, linkParam);
    return TaskChainResponse.builder().chainEnd(false).task(task).build();
  }

  @Override
  public TaskChainResponse executeNextLink(Ambiance ambiance, StepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    BasicHttpChainStepParameters parameters =
        obtainBasicHttpChainStepParameters((BasicHttpChainStepParameters) stepParameters);
    BasicHttpStepParameters linkParam = parameters.getLinkParameters().get(1);
    DelegateTask task = buildTask(ambiance, linkParam);
    return TaskChainResponse.builder().chainEnd(true).task(task).build();
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, StepParameters stepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  private DelegateTask buildTask(Ambiance ambiance, BasicHttpStepParameters linkParam) {
    HttpTaskParameters httpTaskParameters = HttpTaskParameters.builder()
                                                .url(linkParam.getUrl())
                                                .body(linkParam.getBody())
                                                .header(linkParam.getHeader())
                                                .method(linkParam.getMethod())
                                                .socketTimeoutMillis(socketTimeoutMillis)
                                                .build();

    String waitId = generateUuid();
    return DelegateTask.builder()
        .accountId(ambiance.getSetupAbstractions().get("accountId"))
        .waitId(waitId)
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, (String) ambiance.getSetupAbstractions().get("appId"))
        .data(TaskData.builder()
                  .taskType(TaskType.HTTP.name())
                  .parameters(new Object[] {httpTaskParameters})
                  .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                  .build())
        .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, waitId)
        .build();
  }

  @NotNull
  private BasicHttpChainStepParameters obtainBasicHttpChainStepParameters(BasicHttpChainStepParameters parameters) {
    if (isEmpty(parameters.getLinkParameters())) {
      throw new InvalidRequestException("No Chain Links Present");
    }
    return parameters;
  }
}
