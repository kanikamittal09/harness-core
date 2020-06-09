package io.harness.states;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.managerclient.ManagerCIResource;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import io.harness.stateutils.buildstate.BuildSetupUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * This state will setup the build environment, clone the git repository for running CI job.
 */

@Slf4j
public class BuildEnvSetupStep implements Step, SyncExecutable {
  @Inject private ManagerCIResource managerCIResource;
  @Inject private BuildSetupUtils buildSetupUtils;
  public static final StepType STEP_TYPE = BuildEnvSetupStepInfo.typeInfo.getStepType();

  // TODO Async can not be supported at this point. We have to build polling framework on CI manager.
  //     Async will be supported once we will have delegate microservice ready.

  @Override
  public StepResponse executeSync(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs, PassThroughData passThroughData) {
    try {
      BuildEnvSetupStepInfo envSetupStepInfo = (BuildEnvSetupStepInfo) stepParameters;
      // TODO Handle response and fetch cluster from input element
      buildSetupUtils.executeCISetupTask(envSetupStepInfo, ambiance);
      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } catch (Exception e) {
      logger.error("state execution failed", e);
    }
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
