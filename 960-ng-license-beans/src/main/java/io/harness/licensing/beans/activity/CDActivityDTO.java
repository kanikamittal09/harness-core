package io.harness.licensing.beans.activity;

import io.harness.licensing.ModuleType;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class CDActivityDTO extends LicenseActivityDTO {
  long activeServices;

  public ModuleType getModuleType() {
    return ModuleType.CD;
  }
}