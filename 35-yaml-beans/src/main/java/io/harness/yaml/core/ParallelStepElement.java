package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Parallel structure is special list of steps that can be executed in parallel.
 */
@Data
@Builder
@NoArgsConstructor
@JsonTypeName("parallel")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParallelStepElement implements ExecutionWrapper {
  @NotNull List<ExecutionWrapper> sections;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ParallelStepElement(List<ExecutionWrapper> sections) {
    this.sections = sections;
  }
}
