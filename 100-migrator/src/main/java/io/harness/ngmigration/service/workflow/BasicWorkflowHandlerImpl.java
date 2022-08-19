/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.workflow;

import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase.Yaml;
import software.wings.service.impl.yaml.handler.workflow.BasicWorkflowYamlHandler;
import software.wings.yaml.workflow.BasicWorkflowYaml;

import com.google.inject.Inject;
import java.util.List;

public class BasicWorkflowHandlerImpl implements WorkflowHandler {
  @Inject BasicWorkflowYamlHandler basicWorkflowYamlHandler;

  @Override
  public List<Yaml> getPhases(Workflow workflow) {
    BasicWorkflowYaml basicWorkflowYaml = basicWorkflowYamlHandler.toYaml(workflow, workflow.getAppId());
    return basicWorkflowYaml.getPhases();
  }

  @Override
  public List<GraphNode> getSteps(Workflow workflow) {
    BasicOrchestrationWorkflow orchestrationWorkflow = (BasicOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    return getSteps(orchestrationWorkflow.getWorkflowPhases(), orchestrationWorkflow.getPreDeploymentSteps(),
        orchestrationWorkflow.getPostDeploymentSteps());
  }
}
