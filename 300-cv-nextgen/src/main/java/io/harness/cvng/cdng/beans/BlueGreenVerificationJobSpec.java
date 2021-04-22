package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("Bluegreen")
@OwnedBy(HarnessTeam.CV)
@SuperBuilder
@NoArgsConstructor
public class BlueGreenVerificationJobSpec extends BlueGreenCanaryVerificationJobSpec {
  @Override
  public String getType() {
    return "Bluegreen";
  }
}
