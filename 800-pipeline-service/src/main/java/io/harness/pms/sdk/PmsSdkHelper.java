/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.PlanCreationServiceGrpc;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.plan.creation.PlanCreatorServiceInfo;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class PmsSdkHelper {
  @Inject private Map<ModuleType, PlanCreationServiceGrpc.PlanCreationServiceBlockingStub> planCreatorServices;
  @Inject private PmsSdkInstanceService pmsSdkInstanceService;

  /**
   * Gets the list of registered services with their PlanCreatorServiceInfo object
   */
  public Map<String, PlanCreatorServiceInfo> getServices() {
    Map<String, Map<String, Set<String>>> sdkInstances = pmsSdkInstanceService.getInstanceNameToSupportedTypes();
    Map<String, PlanCreatorServiceInfo> services = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(planCreatorServices) && EmptyPredicate.isNotEmpty(sdkInstances)) {
      sdkInstances.forEach((k, v) -> {
        if (planCreatorServices.containsKey(ModuleType.fromString(k))) {
          services.put(k, new PlanCreatorServiceInfo(v, planCreatorServices.get(ModuleType.fromString(k))));
        }
      });
    }
    return services;
  }

  /**
   * Checks if the service supports any of the dependency mentioned.
   */
  public boolean containsSupportedDependencyByYamlPath(
      PlanCreatorServiceInfo serviceInfo, Dependencies dependencies, String serviceName) {
    long start = System.currentTimeMillis();
    if (dependencies == null || EmptyPredicate.isEmpty(dependencies.getDependenciesMap())) {
      return false;
    }

    Map<String, Set<String>> supportedTypes = serviceInfo.getSupportedTypes();
    YamlField fullYamlField;
    try {
      fullYamlField = YamlUtils.readTree(dependencies.getYaml());
    } catch (IOException ex) {
      String message = "Invalid yaml during plan creation";
      log.error(message, ex);
      throw new InvalidRequestException(message);
    }

    boolean supportedDependency = dependencies.getDependenciesMap()
                                      .entrySet()
                                      .stream()
                                      .filter(entry -> {
                                        try {
                                          YamlField field = fullYamlField.fromYamlPath(entry.getValue());
                                          return PlanCreatorUtils.supportsField(supportedTypes, field);
                                        } catch (Exception e) {
                                          log.error("Invalid yaml field", e);
                                          return false;
                                        }
                                      })
                                      .map(Map.Entry::getKey)
                                      .findFirst()
                                      .isPresent();
    log.info("ContainsSupportedDependencyByYamlPath took {}ms for dependencies size {} for serviceName {}",
        System.currentTimeMillis() - start, dependencies.getDependenciesMap().size(), serviceName);
    return supportedDependency;
  }

  /**
   * Checks if the service supports any of the dependency mentioned.
   */
  public boolean containsSupportedSingleDependencyByYamlPath(
      PlanCreatorServiceInfo serviceInfo, YamlField fullYamlField, Map.Entry<String, String> entry) {
    if (entry == null) {
      return false;
    }
    Map<String, Set<String>> supportedTypes = serviceInfo.getSupportedTypes();
    try {
      YamlField field = fullYamlField.fromYamlPath(entry.getValue());
      return PlanCreatorUtils.supportsField(supportedTypes, field);
    } catch (IOException ex) {
      String message = "Invalid yaml during plan creation";
      log.error(message, ex);
      throw new InvalidRequestException(message);
    }
  }

  public Dependencies createBatchDependency(Dependencies dependencies, Map<String, String> dependencyMap) {
    return Dependencies.newBuilder()
        .putAllDependencies(dependencyMap)
        .putAllDependencyMetadata(dependencies.getDependencyMetadataMap())
        .setYaml(dependencies.getYaml())
        .build();
  }
}
