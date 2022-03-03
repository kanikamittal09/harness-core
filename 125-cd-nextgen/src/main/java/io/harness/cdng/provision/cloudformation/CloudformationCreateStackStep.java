/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGResponse;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ng.core.EntityDetail;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.supplier.ThrowingSupplier;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class CloudformationCreateStackStep extends TaskExecutableWithRollbackAndRbac<CloudformationTaskNGResponse> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.CLOUDFORMATION_CREATE_STACK.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  @Inject private KryoSerializer kryoSerializer;
  @Inject private PipelineRbacHelper pipelineRbacHelper;
  @Inject private StepHelper stepHelper;

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    List<EntityDetail> entityDetailList = new ArrayList<>();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);

    // Template file connector
    CloudformationCreateStackStepConfiguration cloudformationStepConfiguration =
        (CloudformationCreateStackStepConfiguration) stepParameters.getSpec();
    if (cloudformationStepConfiguration.getTemplateFilesWrapper().getTemplateFile().getSpec().getType()
        == CloudformationTemplateFileTypes.Remote) {
      RemoteCloudformationTemplateFileSpec remoteTemplateFile =
          (RemoteCloudformationTemplateFileSpec) cloudformationStepConfiguration.getTemplateFilesWrapper()
              .getTemplateFile()
              .getSpec();
      String connectorRef = remoteTemplateFile.getStore().getSpec().getConnectorReference().getValue();
      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(connectorRef, accountId, orgIdentifier, projectIdentifier);
      EntityDetail entityDetail = EntityDetail.builder().type(EntityType.CONNECTORS).entityRef(identifierRef).build();
      entityDetailList.add(entityDetail);
    }
    pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetailList, true);
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    return null;
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<CloudformationTaskNGResponse> responseDataSupplier) throws Exception {
    log.info("Handling Task Result With Security Context for the CreateStack Step");

    return null;
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }
}
