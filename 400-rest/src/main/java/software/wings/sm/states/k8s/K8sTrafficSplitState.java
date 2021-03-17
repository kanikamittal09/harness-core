package software.wings.sm.states.k8s;

import static io.harness.exception.ExceptionUtils.getMessage;

import static software.wings.sm.StateType.K8S_TRAFFIC_SPLIT;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.k8s.model.IstioDestinationWeight;
import io.harness.logging.CommandExecutionStatus;
import io.harness.tasks.ResponseData;

import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTrafficSplitTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import com.github.reinert.jjschema.Attributes;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class K8sTrafficSplitState extends AbstractK8sState {
  @Inject private ActivityService activityService;

  public static final String K8S_TRAFFIC_SPLIT_STATE_NAME = "Traffic Split";

  public K8sTrafficSplitState(String name) {
    super(name, K8S_TRAFFIC_SPLIT.name());
  }

  @Trimmed @Getter @Setter @Attributes(title = "Virtual Service Name") private String virtualServiceName;
  @Getter
  @Setter
  @Attributes(title = "Destination Weights")
  private List<IstioDestinationWeight> istioDestinationWeights = new ArrayList<>();
  @Getter @Setter @Attributes(title = "delegateSelectors") private List<String> delegateSelectors;

  @Override
  public List<String> getDelegateSelectors(ExecutionContext context) {
    return getDelegateSelectors();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      log.info("Executing K8sTrafficSplitState");
      sanitizeStateParameters();

      ContainerInfrastructureMapping infraMapping = k8sStateHelper.fetchContainerInfrastructureMapping(context);
      Activity activity = createActivity(context);

      renderStateVariables(context);

      K8sTaskParameters k8sTaskParameters = K8sTrafficSplitTaskParameters.builder()
                                                .activityId(activity.getUuid())
                                                .commandName(K8S_TRAFFIC_SPLIT_STATE_NAME)
                                                .releaseName(fetchReleaseName(context, infraMapping))
                                                .k8sTaskType(K8sTaskType.TRAFFIC_SPLIT)
                                                .timeoutIntervalInMin(10)
                                                .virtualServiceName(virtualServiceName)
                                                .istioDestinationWeights(istioDestinationWeights)
                                                .build();
      return queueK8sDelegateTask(context, k8sTaskParameters);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      String appId = workflowStandardParams.getAppId();

      K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();
      ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
          ? ExecutionStatus.SUCCESS
          : ExecutionStatus.FAILED;

      activityService.updateStatus(fetchActivityId(context), appId, executionStatus);
      K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
      stateExecutionData.setStatus(executionStatus);

      return ExecutionResponse.builder()
          .stateExecutionData(context.getStateExecutionData())
          .executionStatus(executionStatus)
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  private Activity createActivity(ExecutionContext context) {
    return createK8sActivity(context, K8S_TRAFFIC_SPLIT_STATE_NAME, getStateType(), activityService,
        ImmutableList.of(new K8sDummyCommandUnit(K8sCommandUnitConstants.Init),
            new K8sDummyCommandUnit(K8sCommandUnitConstants.TrafficSplit)));
  }

  private void renderStateVariables(ExecutionContext context) {
    if (isNotBlank(virtualServiceName)) {
      virtualServiceName = context.renderExpression(virtualServiceName);
    }

    for (IstioDestinationWeight ruleWithWeight : istioDestinationWeights) {
      if (isNotBlank(ruleWithWeight.getDestination())) {
        ruleWithWeight.setDestination(context.renderExpression(ruleWithWeight.getDestination()));
      }

      if (isNotBlank(ruleWithWeight.getWeight())) {
        ruleWithWeight.setWeight(context.renderExpression(ruleWithWeight.getWeight()));
      }
    }
  }

  private void sanitizeStateParameters() {
    if (isNotBlank(virtualServiceName)) {
      virtualServiceName = virtualServiceName.trim();
    }

    for (IstioDestinationWeight istioDestinationWeight : istioDestinationWeights) {
      if (isNotBlank(istioDestinationWeight.getDestination())) {
        istioDestinationWeight.setDestination(istioDestinationWeight.getDestination().trim());
      }

      if (isNotBlank(istioDestinationWeight.getWeight())) {
        istioDestinationWeight.setWeight(istioDestinationWeight.getWeight().trim());
      }
    }
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (isBlank(virtualServiceName)) {
      invalidFields.put("VirtualService name", "VirtualService name must not be blank");
    }

    boolean emptyWeight = istioDestinationWeights.stream().anyMatch(
        istioDestinationWeight -> isBlank(istioDestinationWeight.getWeight()));

    if (emptyWeight) {
      invalidFields.put("Weight", "Weight must not be blank");
    }

    boolean emptyDestination = istioDestinationWeights.stream().anyMatch(
        istioDestinationWeight -> isBlank(istioDestinationWeight.getDestination()));

    if (emptyDestination) {
      invalidFields.put("Destination", "Destination must not be blank");
    }

    return invalidFields;
  }

  @Override
  public void validateParameters(ExecutionContext context) {}

  @Override
  public String commandName() {
    return K8S_TRAFFIC_SPLIT_STATE_NAME;
  }

  @Override
  public String stateType() {
    return getStateType();
  }

  @Override
  public List<CommandUnit> commandUnitList(boolean remoteStoreType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ExecutionResponse executeK8sTask(ExecutionContext context, String activityId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response) {
    throw new UnsupportedOperationException();
  }
}
