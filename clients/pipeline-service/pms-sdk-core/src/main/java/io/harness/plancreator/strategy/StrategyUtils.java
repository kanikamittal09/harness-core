/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.strategy;

import static io.harness.expression.EngineExpressionEvaluator.EXPR_END;
import static io.harness.expression.EngineExpressionEvaluator.EXPR_END_ESC;
import static io.harness.expression.EngineExpressionEvaluator.EXPR_START_ESC;
import static io.harness.plancreator.strategy.StrategyConstants.ITEM;
import static io.harness.plancreator.strategy.StrategyConstants.ITERATION;
import static io.harness.plancreator.strategy.StrategyConstants.ITERATIONS;
import static io.harness.plancreator.strategy.StrategyConstants.MATRIX;
import static io.harness.plancreator.strategy.StrategyConstants.REPEAT;
import static io.harness.pms.yaml.YAMLFieldNameConstants.IDENTIFIER;
import static io.harness.pms.yaml.YAMLFieldNameConstants.NAME;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STAGES;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEPS;
import static io.harness.strategy.StrategyValidationUtils.STRATEGY_IDENTIFIER_POSTFIX_ESCAPED;

import io.harness.advisers.nextstep.NextStepAdviserParameters;
import io.harness.jackson.JsonNodeUtils;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.EdgeLayoutList;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.matrix.StrategyConstants;
import io.harness.steps.matrix.StrategyMetadata;
import io.harness.strategy.StrategyValidationUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StrategyUtils {
  public boolean isWrappedUnderStrategy(YamlField yamlField) {
    YamlField strategyField = yamlField.getNode().getField(YAMLFieldNameConstants.STRATEGY);
    return strategyField != null;
  }

  public String getSwappedPlanNodeId(PlanCreationContext ctx, String originalPlanNodeId) {
    YamlField strategyField = ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.STRATEGY);
    // Since strategy is a child of stage but in execution we want to wrap stage around strategy,
    // we are swapping the uuid of stage and strategy node.
    String planNodeId = originalPlanNodeId;
    if (strategyField != null) {
      planNodeId = strategyField.getNode().getUuid();
    }
    return planNodeId;
  }

  public String getIdentifierWithExpression(PlanCreationContext ctx, String originalIdentifier) {
    YamlField strategyField = ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.STRATEGY);
    // Since strategy is a child of stage but in execution we want to wrap stage around strategy,
    // we are appending an expression that will be resolved during execution
    String identifier = originalIdentifier;
    if (strategyField != null) {
      identifier = originalIdentifier + StrategyValidationUtils.STRATEGY_IDENTIFIER_POSTFIX;
    }
    return identifier;
  }

  public List<AdviserObtainment> getAdviserObtainments(
      YamlField stageField, KryoSerializer kryoSerializer, boolean checkForStrategy) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (stageField != null && stageField.getNode() != null) {
      // if parent is parallel, then we need not add nextStepAdvise as all the executions will happen in parallel
      if (stageField.checkIfParentIsParallel(STAGES)) {
        return adviserObtainments;
      }
      if (checkForStrategy && isWrappedUnderStrategy(stageField)) {
        return adviserObtainments;
      }
      YamlField siblingField = stageField.getNode().nextSiblingFromParentArray(
          stageField.getName(), Arrays.asList(YAMLFieldNameConstants.STAGE, YAMLFieldNameConstants.PARALLEL));
      if (siblingField != null && siblingField.getNode().getUuid() != null) {
        adviserObtainments.add(
            AdviserObtainment.newBuilder()
                .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.NEXT_STAGE.name()).build())
                .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                    NextStepAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
                .build());
      }
    }
    return adviserObtainments;
  }

  public Map<String, GraphLayoutNode> modifyStageLayoutNodeGraph(YamlField yamlField) {
    Map<String, GraphLayoutNode> stageYamlFieldMap = new LinkedHashMap<>();
    YamlField siblingField = yamlField.getNode().nextSiblingFromParentArray(
        yamlField.getName(), Arrays.asList(YAMLFieldNameConstants.STAGE, YAMLFieldNameConstants.PARALLEL));
    EdgeLayoutList edgeLayoutList;
    String planNodeId = yamlField.getNode().getField(YAMLFieldNameConstants.STRATEGY).getNode().getUuid();
    if (siblingField == null) {
      edgeLayoutList = EdgeLayoutList.newBuilder().addCurrentNodeChildren(planNodeId).build();
    } else {
      edgeLayoutList = EdgeLayoutList.newBuilder()
                           .addNextIds(siblingField.getNode().getUuid())
                           .addCurrentNodeChildren(planNodeId)
                           .build();
    }

    StrategyType strategyType = StrategyType.LOOP;
    if (yamlField.getNode().getField(YAMLFieldNameConstants.STRATEGY).getNode().getField("matrix") != null) {
      strategyType = StrategyType.MATRIX;
    } else if (yamlField.getNode().getField(YAMLFieldNameConstants.STRATEGY).getNode().getField("parallelism")
        != null) {
      strategyType = StrategyType.PARALLELISM;
    }
    stageYamlFieldMap.put(yamlField.getNode().getUuid(),
        GraphLayoutNode.newBuilder()
            .setNodeUUID(yamlField.getNode().getUuid())
            .setNodeType(strategyType.name())
            .setName(yamlField.getNode().getName())
            .setNodeGroup(StepOutcomeGroup.STRATEGY.name())
            .setNodeIdentifier(yamlField.getNode().getIdentifier())
            .setEdgeLayoutList(edgeLayoutList)
            .build());
    stageYamlFieldMap.put(planNodeId,
        GraphLayoutNode.newBuilder()
            .setNodeUUID(planNodeId)
            .setNodeType(yamlField.getNode().getType())
            .setName(yamlField.getNode().getName())
            .setNodeGroup(StepOutcomeGroup.STAGE.name())
            .setNodeIdentifier(yamlField.getNode().getIdentifier())
            .setEdgeLayoutList(EdgeLayoutList.newBuilder().build())
            .build());
    return stageYamlFieldMap;
  }

  public List<AdviserObtainment> getAdviserObtainmentFromMetaDataForStep(
      KryoSerializer kryoSerializer, YamlField currentField) {
    if (currentField.checkIfParentIsParallel(STEPS)) {
      return new ArrayList<>();
    }
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (currentField != null && currentField.getNode() != null) {
      YamlField siblingField = currentField.getNode().nextSiblingFromParentArray(currentField.getName(),
          Arrays.asList(
              YAMLFieldNameConstants.STEP, YAMLFieldNameConstants.STEP_GROUP, YAMLFieldNameConstants.PARALLEL));
      if (siblingField != null && siblingField.getNode().getUuid() != null) {
        AdviserObtainment adviserObtainment =
            AdviserObtainment.newBuilder()
                .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.NEXT_STEP.name()).build())
                .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                    NextStepAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
                .build();
        adviserObtainments.add(adviserObtainment);
      }
    }
    return adviserObtainments;
  }

  public void addStrategyFieldDependencyIfPresent(KryoSerializer kryoSerializer, PlanCreationContext ctx, String uuid,
      String name, String identifier, LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap,
      Map<String, ByteString> metadataMap, List<AdviserObtainment> adviserObtainments) {
    YamlField strategyField = ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.STRATEGY);
    if (strategyField != null) {
      // This is mandatory because it is the parent's responsibility to pass the nodeId and the childNodeId to the
      // strategy node
      metadataMap.put(StrategyConstants.STRATEGY_METADATA + strategyField.getNode().getUuid(),
          ByteString.copyFrom(kryoSerializer.asDeflatedBytes(StrategyMetadata.builder()
                                                                 .strategyNodeId(uuid)
                                                                 .adviserObtainments(adviserObtainments)
                                                                 .childNodeId(strategyField.getNode().getUuid())
                                                                 .strategyNodeName(refineIdentifier(name))
                                                                 .strategyNodeIdentifier(refineIdentifier(identifier))
                                                                 .build())));
      planCreationResponseMap.put(uuid,
          PlanCreationResponse.builder()
              .dependencies(
                  DependenciesUtils.toDependenciesProto(ImmutableMap.of(uuid, strategyField))
                      .toBuilder()
                      .putDependencyMetadata(uuid, Dependency.newBuilder().putAllMetadata(metadataMap).build())
                      .build())
              .build());
    }
  }

  public void addStrategyFieldDependencyIfPresent(KryoSerializer kryoSerializer, PlanCreationContext ctx,
      String fieldUuid, String fieldIdentifier, String fieldName, Map<String, YamlField> dependenciesNodeMap,
      Map<String, ByteString> metadataMap, List<AdviserObtainment> adviserObtainments) {
    YamlField strategyField = ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.STRATEGY);
    if (strategyField != null) {
      dependenciesNodeMap.put(fieldUuid, strategyField);
      // This is mandatory because it is the parent's responsibility to pass the nodeId and the childNodeId to the
      // strategy node
      metadataMap.put(StrategyConstants.STRATEGY_METADATA + strategyField.getNode().getUuid(),
          ByteString.copyFrom(
              kryoSerializer.asDeflatedBytes(StrategyMetadata.builder()
                                                 .strategyNodeId(fieldUuid)
                                                 .adviserObtainments(adviserObtainments)
                                                 .childNodeId(strategyField.getNode().getUuid())
                                                 .strategyNodeIdentifier(refineIdentifier(fieldIdentifier))
                                                 .strategyNodeName(refineIdentifier(fieldName))
                                                 .build())));
    }
  }

  public void modifyJsonNode(JsonNode jsonNode, List<String> combinations) {
    String newIdentifier = jsonNode.get(IDENTIFIER).asText() + "_" + String.join("_", combinations);
    String newName = jsonNode.get(NAME).asText() + "_" + String.join("_", combinations);
    JsonNodeUtils.updatePropertyInObjectNode(jsonNode, IDENTIFIER, newIdentifier);
    JsonNodeUtils.updatePropertyInObjectNode(jsonNode, NAME, newName);
    // Remove strategy node so that we don't calculate strategy on it again in the future.
    JsonNodeUtils.deletePropertiesInJsonNode((ObjectNode) jsonNode, "strategy");
  }

  public String replaceExpressions(
      String jsonString, Map<String, String> combinations, int currentIteration, int totalIteration, String itemValue) {
    Map<String, String> expressions = createExpressions(combinations, currentIteration, totalIteration, itemValue);
    String result = jsonString;
    for (Map.Entry<String, String> expression : expressions.entrySet()) {
      result = result.replaceAll(expression.getKey(), expression.getValue());
    }
    return result;
  }
  public Map<String, String> createExpressions(
      Map<String, String> combinations, int currentIteration, int totalIteration, String itemValue) {
    Map<String, String> expressionsMap = new HashMap<>();
    String matrixExpression = EXPR_START_ESC + "matrix.%s" + EXPR_END_ESC;
    String strategyMatrixExpression = EXPR_START_ESC + "strategy.matrix.%s" + EXPR_END_ESC;
    String repeatExpression = EXPR_START_ESC + "repeat.item" + EXPR_END_ESC;

    for (Map.Entry<String, String> entry : combinations.entrySet()) {
      expressionsMap.put(String.format(matrixExpression, entry.getKey()), entry.getValue());
      expressionsMap.put(String.format(strategyMatrixExpression, entry.getKey()), entry.getValue());
    }
    expressionsMap.put(EXPR_START_ESC + "strategy.iteration" + EXPR_END_ESC, String.valueOf(currentIteration));
    expressionsMap.put(EXPR_START_ESC + "strategy.iterations" + EXPR_END, String.valueOf(totalIteration));
    expressionsMap.put(repeatExpression, itemValue == null ? "" : itemValue);
    return expressionsMap;
  }

  /**
   * This function remove <+strategy.identifierPostFix> if present on the passed string
   */
  private String refineIdentifier(String identifier) {
    return identifier.replaceAll(STRATEGY_IDENTIFIER_POSTFIX_ESCAPED, "");
  }

  /**
   * This is used to fetch strategy object map at a given level
   * @param level
   * @return
   */
  public Map<String, Object> fetchStrategyObjectMap(Level level) {
    Map<String, Object> strategyObjectMap = new HashMap<>();
    if (level.hasStrategyMetadata()) {
      return fetchStrategyObjectMap(Lists.newArrayList(level));
    }
    return strategyObjectMap;
  }

  /**
   * This is used to fetch the strategy object map from different values. It combines the axis of all it's parent
   *
   * @param levelsWithStrategyMetadata
   * @return
   */
  public Map<String, Object> fetchStrategyObjectMap(List<Level> levelsWithStrategyMetadata) {
    Map<String, Object> strategyObjectMap = new HashMap<>();
    Map<String, String> matrixValuesMap = new HashMap<>();
    Map<String, String> repeatValuesMap = new HashMap<>();

    for (Level level : levelsWithStrategyMetadata) {
      if (level.getStrategyMetadata().hasMatrixMetadata()) {
        matrixValuesMap.putAll(level.getStrategyMetadata().getMatrixMetadata().getMatrixValuesMap());
      }
      if (level.getStrategyMetadata().hasForMetadata()) {
        repeatValuesMap.put(ITEM, level.getStrategyMetadata().getForMetadata().getValue());
      }
      strategyObjectMap.put(ITERATION, level.getStrategyMetadata().getCurrentIteration());
      strategyObjectMap.put(ITERATIONS, level.getStrategyMetadata().getTotalIterations());
      strategyObjectMap.put("identifierPostFix", AmbianceUtils.getStrategyPostfix(level));
    }
    strategyObjectMap.put(MATRIX, matrixValuesMap);
    strategyObjectMap.put(REPEAT, repeatValuesMap);

    return strategyObjectMap;
  }
}
