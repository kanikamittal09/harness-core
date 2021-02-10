package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("k8sDeleteStepParameters")
public class K8sDeleteStepParameters extends K8sDeleteBaseStepInfo implements K8sStepParameters {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public K8sDeleteStepParameters(DeleteResourcesWrapper deleteResources, ParameterField<String> timeout,
      ParameterField<Boolean> skipDryRun, RollbackInfo rollbackInfo) {
    super(deleteResources, skipDryRun);
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
  }
}
