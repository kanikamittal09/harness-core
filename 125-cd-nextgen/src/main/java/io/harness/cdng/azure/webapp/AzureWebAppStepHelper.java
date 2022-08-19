/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.azure.webapp;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants.APPLICATION_SETTINGS;
import static io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants.CONNECTION_STRINGS;
import static io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants.STARTUP_COMMAND;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.connector.docker.DockerAuthType.ANONYMOUS;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.ACR_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.ARTIFACTORY_REGISTRY_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.DOCKER_REGISTRY_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.ECR_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.GCR_NAME;
import static io.harness.delegate.task.artifacts.ArtifactSourceConstants.NEXUS3_REGISTRY_NAME;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.FileReference;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.artifact.outcome.AcrArtifactOutcome;
import io.harness.cdng.artifact.outcome.ArtifactOutcome;
import io.harness.cdng.artifact.outcome.ArtifactoryArtifactOutcome;
import io.harness.cdng.artifact.outcome.DockerArtifactOutcome;
import io.harness.cdng.artifact.outcome.EcrArtifactOutcome;
import io.harness.cdng.artifact.outcome.GcrArtifactOutcome;
import io.harness.cdng.artifact.outcome.NexusArtifactOutcome;
import io.harness.cdng.azure.AzureHelperService;
import io.harness.cdng.azure.config.ApplicationSettingsOutcome;
import io.harness.cdng.azure.config.ConnectionStringsOutcome;
import io.harness.cdng.azure.config.StartupCommandOutcome;
import io.harness.cdng.azure.webapp.beans.AzureWebAppPreDeploymentDataOutput;
import io.harness.cdng.expressions.CDExpressionResolver;
import io.harness.cdng.infra.beans.AzureWebAppInfrastructureOutcome;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.settings.AppSettingsFile;
import io.harness.delegate.task.azure.appservice.settings.EncryptedAppSettingsFile;
import io.harness.delegate.task.azure.appservice.webapp.ng.AzureWebAppInfraDelegateConfig;
import io.harness.delegate.task.azure.artifact.AzureArtifactConfig;
import io.harness.delegate.task.azure.artifact.AzureContainerArtifactConfig;
import io.harness.delegate.task.azure.artifact.AzureContainerArtifactConfig.AzureContainerArtifactConfigBuilder;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filestore.dto.node.FileNodeDTO;
import io.harness.filestore.dto.node.FileStoreNodeDTO;
import io.harness.filestore.dto.node.FolderNodeDTO;
import io.harness.filestore.service.FileStoreService;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.api.NGEncryptedDataService;
import io.harness.ng.core.infrastructure.InfrastructureKind;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.yaml.ParameterField;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.utils.IdentifierRefHelper;

import software.wings.beans.TaskType;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class AzureWebAppStepHelper {
  @Inject private OutcomeService outcomeService;
  @Inject private FileStoreService fileStoreService;
  @Inject private CDStepHelper cdStepHelper;
  @Inject private AzureHelperService azureHelperService;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private CDExpressionResolver cdExpressionResolver;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private NGEncryptedDataService ngEncryptedDataService;
  @Named("PRIVILEGED") @Inject private SecretManagerClientService secretManagerClientService;

  public AzureAppServicePreDeploymentData getPreDeploymentData(Ambiance ambiance, String sweepingOutputName) {
    OptionalSweepingOutput sweepingOutput = executionSweepingOutputService.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(sweepingOutputName));
    if (sweepingOutput.isFound()) {
      return ((AzureWebAppPreDeploymentDataOutput) sweepingOutput.getOutput()).getPreDeploymentData();
    }
    return null;
  }

  public Map<String, StoreConfig> fetchWebAppConfig(Ambiance ambiance) {
    Map<String, StoreConfig> settingsConfig = new HashMap<>();
    OptionalOutcome startupCommandOutcome =
        outcomeService.resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(STARTUP_COMMAND));
    OptionalOutcome applicationSettingsOutcome =
        outcomeService.resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(APPLICATION_SETTINGS));
    OptionalOutcome connectionStringsOutcome =
        outcomeService.resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(CONNECTION_STRINGS));

    if (startupCommandOutcome.isFound()) {
      StartupCommandOutcome startupCommand = (StartupCommandOutcome) startupCommandOutcome.getOutcome();
      settingsConfig.put(STARTUP_COMMAND, startupCommand.getStore());
    }

    if (applicationSettingsOutcome.isFound()) {
      ApplicationSettingsOutcome applicationSettings =
          (ApplicationSettingsOutcome) applicationSettingsOutcome.getOutcome();
      settingsConfig.put(APPLICATION_SETTINGS, applicationSettings.getStore());
    }

    if (connectionStringsOutcome.isFound()) {
      ConnectionStringsOutcome connectionStrings = (ConnectionStringsOutcome) connectionStringsOutcome.getOutcome();
      settingsConfig.put(CONNECTION_STRINGS, connectionStrings.getStore());
    }

    return settingsConfig;
  }

  public TaskRequest prepareGitFetchTaskRequest(StepElementParameters stepElementParameters, Ambiance ambiance,
      Map<String, GitStoreConfig> gitStoreConfigs, List<String> units) {
    List<GitFetchFilesConfig> gitFetchFilesConfigs = new ArrayList<>();
    for (Map.Entry<String, GitStoreConfig> configEntry : gitStoreConfigs.entrySet()) {
      gitFetchFilesConfigs.add(cdStepHelper.getGitFetchFilesConfig(
          ambiance, configEntry.getValue(), configEntry.getKey(), configEntry.getKey()));
    }

    String accountId = AmbianceUtils.getAccountId(ambiance);
    GitFetchRequest gitFetchRequest = GitFetchRequest.builder()
                                          .accountId(accountId)
                                          .gitFetchFilesConfigs(gitFetchFilesConfigs)
                                          .shouldOpenLogStream(true)
                                          .closeLogStream(true)
                                          .build();

    return prepareTaskRequest(
        stepElementParameters, ambiance, gitFetchRequest, TaskType.GIT_FETCH_NEXT_GEN_TASK, units);
  }

  public TaskRequest prepareTaskRequest(StepElementParameters stepElementParameters, Ambiance ambiance,
      TaskParameters taskParameters, TaskType taskType, List<String> units) {
    AzureWebAppStepParameters stepSpec = (AzureWebAppStepParameters) stepElementParameters.getSpec();
    List<TaskSelectorYaml> taskSelectors = stepSpec.getDelegateSelectors().getValue();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(CDStepHelper.getTimeoutInMillis(stepElementParameters))
                                  .taskType(taskType.name())
                                  .parameters(new Object[] {taskParameters})
                                  .build();

    return cdStepHelper.prepareTaskRequest(ambiance, taskData, units, taskType.getDisplayName(),
        TaskSelectorYaml.toTaskSelector(emptyIfNull(taskSelectors)));
  }

  public Map<String, AppSettingsFile> fetchWebAppConfigsFromHarnessStore(
      Ambiance ambiance, Map<String, HarnessStore> harnessStoreConfigs) {
    return harnessStoreConfigs.entrySet().stream().collect(Collectors.toMap(
        Map.Entry::getKey, entry -> fetchFileContentFromHarnessStore(ambiance, entry.getKey(), entry.getValue())));
  }

  public AzureWebAppInfraDelegateConfig getInfraDelegateConfig(
      Ambiance ambiance, String webApp, String deploymentSlot) {
    InfrastructureOutcome infrastructureOutcome = cdStepHelper.getInfrastructureOutcome(ambiance);
    if (!(infrastructureOutcome instanceof AzureWebAppInfrastructureOutcome)) {
      throw new InvalidArgumentsException(Pair.of("infrastructure",
          format("Invalid infrastructure type: %s, expected: %s", infrastructureOutcome.getKind(),
              InfrastructureKind.AZURE_WEB_APP)));
    }

    AzureWebAppInfrastructureOutcome infrastructure = (AzureWebAppInfrastructureOutcome) infrastructureOutcome;
    return getInfraDelegateConfig(ambiance, infrastructure, webApp, deploymentSlot);
  }

  public AzureWebAppInfraDelegateConfig getInfraDelegateConfig(
      Ambiance ambiance, AzureWebAppInfrastructureOutcome infrastructure, String webApp, String deploymentSlot) {
    ConnectorInfoDTO connectorInfo = cdStepHelper.getConnector(infrastructure.getConnectorRef(), ambiance);
    if (!(connectorInfo.getConnectorConfig() instanceof AzureConnectorDTO)) {
      throw new InvalidArgumentsException(Pair.of("infrastructure",
          format("Invalid infrastructure connector type: %s, expected: %s",
              connectorInfo.getConnectorType().getDisplayName(), ConnectorType.AZURE.getDisplayName())));
    }

    AzureConnectorDTO azureConnectorDTO = (AzureConnectorDTO) connectorInfo.getConnectorConfig();
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    return AzureWebAppInfraDelegateConfig.builder()
        .azureConnectorDTO(azureConnectorDTO)
        .subscription(infrastructure.getSubscription())
        .resourceGroup(infrastructure.getResourceGroup())
        .appName(webApp)
        .deploymentSlot(AzureResourceUtility.fixDeploymentSlotName(deploymentSlot, webApp))
        .encryptionDataDetails(azureHelperService.getEncryptionDetails(azureConnectorDTO, ngAccess))
        .build();
  }

  public AzureArtifactConfig getPrimaryArtifactConfig(Ambiance ambiance) {
    ArtifactOutcome artifactOutcome = cdStepHelper.resolveArtifactsOutcome(ambiance).orElseThrow(
        () -> new InvalidArgumentsException(Pair.of("artifacts", "Artifact is required for Azure WebApp")));
    switch (artifactOutcome.getArtifactType()) {
      case DOCKER_REGISTRY_NAME:
      case ECR_NAME:
      case GCR_NAME:
      case ACR_NAME:
      case NEXUS3_REGISTRY_NAME:
      case ARTIFACTORY_REGISTRY_NAME:
        return getAzureContainerArtifactConfig(ambiance, artifactOutcome);

      default:
        throw new InvalidArgumentsException(Pair.of("artifacts",
            format("Artifact type %s is not yet supported in Azure WebApp", artifactOutcome.getArtifactType())));
    }
  }

  public Map<String, AppSettingsFile> getConfigValuesFromGitFetchResponse(
      Ambiance ambiance, GitFetchResponse gitFetchResponse) {
    return gitFetchResponse.getFilesFromMultipleRepo()
        .entrySet()
        .stream()
        .filter(entry -> isNotEmpty(entry.getValue().getFiles()))
        .collect(Collectors.toMap(Map.Entry::getKey,
            entry
            -> AppSettingsFile.create(engineExpressionService.renderExpression(
                ambiance, entry.getValue().getFiles().get(0).getFileContent()))));
  }

  public static <T extends StoreConfig> Map<String, T> filterAndMapConfigs(
      Map<String, StoreConfig> configs, Predicate<String> kindTest) {
    return configs.entrySet()
        .stream()
        .filter(entry -> kindTest.test(entry.getValue().getKind()))
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> (T) entry.getValue()));
  }

  public static <T extends StoreConfig, U extends StoreConfig> Map<String, StoreConfig> getConfigDifference(
      Map<String, T> aConfigs, Map<String, U> bConfigs) {
    return Sets.difference(aConfigs.entrySet(), bConfigs.entrySet())
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private AzureArtifactConfig getAzureContainerArtifactConfig(Ambiance ambiance, ArtifactOutcome artifactOutcome) {
    ConnectorInfoDTO connectorInfo;
    AzureContainerArtifactConfigBuilder artifactConfigBuilder = AzureContainerArtifactConfig.builder();

    switch (artifactOutcome.getArtifactType()) {
      case DOCKER_REGISTRY_NAME:
        DockerArtifactOutcome dockerArtifactOutcome = (DockerArtifactOutcome) artifactOutcome;
        connectorInfo = cdStepHelper.getConnector(dockerArtifactOutcome.getConnectorRef(), ambiance);
        artifactConfigBuilder.registryType(
            getAzureRegistryType((DockerConnectorDTO) connectorInfo.getConnectorConfig()));
        artifactConfigBuilder.image(dockerArtifactOutcome.getImage());
        artifactConfigBuilder.tag(dockerArtifactOutcome.getTag());
        break;
      case ACR_NAME:
        AcrArtifactOutcome acrArtifactOutcome = (AcrArtifactOutcome) artifactOutcome;
        connectorInfo = cdStepHelper.getConnector(acrArtifactOutcome.getConnectorRef(), ambiance);
        artifactConfigBuilder.registryType(AzureRegistryType.ACR);
        artifactConfigBuilder.image(acrArtifactOutcome.getImage());
        artifactConfigBuilder.tag(acrArtifactOutcome.getTag());
        artifactConfigBuilder.registryHostname(acrArtifactOutcome.getRegistry());
        break;
      case ECR_NAME:
        EcrArtifactOutcome ecrArtifactOutcome = (EcrArtifactOutcome) artifactOutcome;
        connectorInfo = cdStepHelper.getConnector(ecrArtifactOutcome.getConnectorRef(), ambiance);
        artifactConfigBuilder.registryType(AzureRegistryType.ECR);
        artifactConfigBuilder.image(ecrArtifactOutcome.getImage());
        artifactConfigBuilder.tag(ecrArtifactOutcome.getTag());
        artifactConfigBuilder.region(ecrArtifactOutcome.getRegion());
        break;
      case GCR_NAME:
        GcrArtifactOutcome gcrArtifactOutcome = (GcrArtifactOutcome) artifactOutcome;
        connectorInfo = cdStepHelper.getConnector(gcrArtifactOutcome.getConnectorRef(), ambiance);
        artifactConfigBuilder.registryType(AzureRegistryType.GCR);
        artifactConfigBuilder.image(gcrArtifactOutcome.getImage());
        artifactConfigBuilder.tag(gcrArtifactOutcome.getTag());
        artifactConfigBuilder.registryHostname(gcrArtifactOutcome.getRegistryHostname());
        break;
      case NEXUS3_REGISTRY_NAME:
        NexusArtifactOutcome nexusArtifactOutcome = (NexusArtifactOutcome) artifactOutcome;
        connectorInfo = cdStepHelper.getConnector(nexusArtifactOutcome.getConnectorRef(), ambiance);
        artifactConfigBuilder.registryType(AzureRegistryType.NEXUS_PRIVATE_REGISTRY);
        artifactConfigBuilder.image(nexusArtifactOutcome.getImage());
        artifactConfigBuilder.tag(nexusArtifactOutcome.getTag());
        artifactConfigBuilder.registryHostname(nexusArtifactOutcome.getRegistryHostname());
        break;
      case ARTIFACTORY_REGISTRY_NAME:
        ArtifactoryArtifactOutcome artifactoryArtifactOutcome = (ArtifactoryArtifactOutcome) artifactOutcome;
        connectorInfo = cdStepHelper.getConnector(artifactoryArtifactOutcome.getConnectorRef(), ambiance);
        artifactConfigBuilder.registryType(AzureRegistryType.ARTIFACTORY_PRIVATE_REGISTRY);
        artifactConfigBuilder.image(artifactoryArtifactOutcome.getImage());
        artifactConfigBuilder.tag(artifactoryArtifactOutcome.getTag());
        artifactConfigBuilder.registryHostname(artifactoryArtifactOutcome.getRegistryHostname());
        break;
      default:
        throw new InvalidArgumentsException(
            Pair.of("artifacts", format("Unsupported artifact type %s", artifactOutcome.getArtifactType())));
    }

    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    List<DecryptableEntity> decryptableEntities = connectorInfo.getConnectorConfig().getDecryptableEntities();
    if (decryptableEntities != null) {
      for (DecryptableEntity decryptableEntity : decryptableEntities) {
        encryptedDataDetails.addAll(secretManagerClientService.getEncryptionDetails(ngAccess, decryptableEntity));
      }
    }

    return artifactConfigBuilder.connectorConfig(connectorInfo.getConnectorConfig())
        .encryptedDataDetails(encryptedDataDetails)
        .build();
  }

  private AzureRegistryType getAzureRegistryType(DockerConnectorDTO dockerConfig) {
    if (dockerConfig.getAuth().getAuthType().equals(ANONYMOUS)) {
      return AzureRegistryType.DOCKER_HUB_PUBLIC;
    } else {
      return AzureRegistryType.DOCKER_HUB_PRIVATE;
    }
  }

  private AppSettingsFile fetchFileContentFromHarnessStore(
      Ambiance ambiance, String settingsType, HarnessStore harnessStore) {
    HarnessStore renderedHarnessStore = (HarnessStore) cdExpressionResolver.updateExpressions(ambiance, harnessStore);
    if (!ParameterField.isNull(renderedHarnessStore.getFiles())
        && isNotEmpty(renderedHarnessStore.getFiles().getValue())) {
      List<String> harnessStoreFiles = renderedHarnessStore.getFiles().getValue();
      String firstFile = harnessStoreFiles.stream().findFirst().orElseThrow(
          () -> new InvalidArgumentsException(Pair.of(settingsType, "No file configured for harness file store")));
      return fetchFileContentFromFileStore(ambiance, settingsType, firstFile);
    } else if (!ParameterField.isNull(renderedHarnessStore.getSecretFiles())
        && isNotEmpty(renderedHarnessStore.getSecretFiles().getValue())) {
      List<String> harnessStoreSecretFiles = renderedHarnessStore.getSecretFiles().getValue();
      String firstSecretFile = harnessStoreSecretFiles.stream().findFirst().orElseThrow(
          () -> new InvalidArgumentsException(Pair.of(settingsType, "No secret file configured for harness store")));
      return fetchSecretFile(ambiance, settingsType, firstSecretFile);
    }

    throw new InvalidArgumentsException(Pair.of(settingsType, "Either 'files' or 'secretFiles' is required"));
  }

  private AppSettingsFile fetchFileContentFromFileStore(Ambiance ambiance, String settingsType, String filePath) {
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    FileReference fileReference = FileReference.of(
        filePath, ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    FileStoreNodeDTO fileStoreNodeDTO =
        fileStoreService
            .getWithChildrenByPath(fileReference.getAccountIdentifier(), fileReference.getOrgIdentifier(),
                fileReference.getProjectIdentifier(), fileReference.getPath(), true)
            .orElseThrow(()
                             -> new InvalidArgumentsException(
                                 Pair.of(settingsType, format("File '%s' doesn't exists", fileReference.getPath()))));

    if (fileStoreNodeDTO instanceof FolderNodeDTO) {
      throw new InvalidArgumentsException(
          Pair.of(settingsType, format("Provided path '%s' is a folder, expecting a file", fileReference.getPath())));
    }

    if (fileStoreNodeDTO instanceof FileNodeDTO) {
      FileNodeDTO fileNode = (FileNodeDTO) fileStoreNodeDTO;
      if (isNotEmpty(fileNode.getContent())) {
        return AppSettingsFile.create(engineExpressionService.renderExpression(ambiance, fileNode.getContent()));
      }

      log.warn("Received empty or null content for file: {}", fileStoreNodeDTO.getPath());
      return AppSettingsFile.create("");
    }

    log.error("Unknown file store node: {}", fileStoreNodeDTO.getClass().getSimpleName());
    throw new InvalidArgumentsException(Pair.of(settingsType, "Unsupported file store node"));
  }

  private AppSettingsFile fetchSecretFile(Ambiance ambiance, String settingsType, String secretRef) {
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    IdentifierRef fileRef = IdentifierRefHelper.getIdentifierRef(
        secretRef, ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    EncryptedAppSettingsFile encryptedAppSettingsFile =
        EncryptedAppSettingsFile.builder()
            .secretFileReference(SecretRefHelper.createSecretRef(fileRef.getIdentifier()))
            .build();
    List<EncryptedDataDetail> encryptedDataDetails =
        ngEncryptedDataService.getEncryptionDetails(ngAccess, encryptedAppSettingsFile);
    if (encryptedDataDetails == null) {
      throw new InvalidArgumentsException(
          Pair.of(settingsType, format("No encrypted data details found for secret file %s", secretRef)));
    }

    return AppSettingsFile.create(encryptedAppSettingsFile, encryptedDataDetails);
  }
}
