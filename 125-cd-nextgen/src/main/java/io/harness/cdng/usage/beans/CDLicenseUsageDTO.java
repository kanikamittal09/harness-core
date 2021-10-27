package io.harness.cdng.usage.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.types.CDLicenseType;
import io.harness.licensing.usage.beans.LicenseUsageDTO;
import io.harness.licensing.usage.beans.UsageDataDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@OwnedBy(HarnessTeam.CDC)
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = ServiceInstanceUsageDTO.class, name = "SERVICE_INSTANCES")
      , @JsonSubTypes.Type(value = ServiceUsageDTO.class, name = "SERVICES")
    })
public class CDLicenseUsageDTO extends LicenseUsageDTO {
  UsageDataDTO activeServices;
  UsageDataDTO activeServiceInstances;
  CDLicenseType cdLicenseType;

  @Override
  public String getModule() {
    return ModuleType.CD.toString();
  }
}
