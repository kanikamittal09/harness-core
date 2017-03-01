package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * Created by brett on 3/1/17
 */
public class GkeAutoScaleProvisionState extends State {
  public GkeAutoScaleProvisionState(String name) {
    super(name, StateType.GKE_AUTOSCALE_PROVISION.name());
  }

  private String gkeComputeProviderId;
  private String autoScaleGroupNamePattern;
  private String launchConfigurationName;
  // TODO - more relevant attribute

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return null;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
