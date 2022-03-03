/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.experimental.Wither;

public class CloudformationParametersFilesWrapper {
  @NotNull @Wither @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) private ParameterField<String> name;
  @NotNull @Wither @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) private ParameterField<String> value;
  @NotNull @Wither @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) private ParameterField<String> type;
}
