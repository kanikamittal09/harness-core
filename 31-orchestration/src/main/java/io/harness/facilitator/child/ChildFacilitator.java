package io.harness.facilitator.child;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.FacilitatorType;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.state.io.StateTransput;

import java.util.List;

@Redesign
@Produces(Facilitator.class)
public class ChildFacilitator implements Facilitator {
  @Override
  public FacilitatorType getType() {
    return FacilitatorType.builder().type(FacilitatorType.CHILD).build();
  }

  @Override
  public FacilitatorResponse facilitate(
      Ambiance ambiance, FacilitatorParameters parameters, List<StateTransput> inputs) {
    return FacilitatorResponse.builder()
        .executionMode(ExecutionMode.CHILD)
        .initialWait(parameters.getWaitDurationSeconds())
        .build();
  }
}
