package io.harness.gitsync.core.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.FullSyncResponse;
import io.harness.gitsync.FullSyncServiceGrpc;
import io.harness.gitsync.common.helper.GitSyncGrpcClientUtils;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.ng.core.entitydetail.EntityDetailRestToProtoMapper;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitFullSyncProcessorServiceImpl implements io.harness.gitsync.core.fullsync.GitFullSyncProcessorService {
  Map<Microservice, FullSyncServiceGrpc.FullSyncServiceBlockingStub> fullSyncServiceBlockingStubMap;
  YamlGitConfigService yamlGitConfigService;
  EntityDetailRestToProtoMapper entityDetailRestToProtoMapper;
  io.harness.gitsync.core.fullsync.GitFullSyncEntityService gitFullSyncEntityService;

  private static int MAX_RETRY_COUNT = 2;

  @Override
  public void processFile(GitFullSyncEntityInfo entityInfo) {
    boolean failed = false;
    FullSyncResponse fullSyncResponse = null;
    try {
      fullSyncResponse = performSyncForEntity(entityInfo);
      failed = !fullSyncResponse.getSuccess();
    } catch (Exception e) {
      failed = true;
    }
    if (failed) {
      String errorMsg = "";
      if (fullSyncResponse != null) {
        errorMsg = fullSyncResponse.getErrorMsg();
      }
      gitFullSyncEntityService.markQueuedOrFailed(entityInfo.getMessageId(), entityInfo.getAccountIdentifier(),
          entityInfo.getRetryCount(), MAX_RETRY_COUNT, errorMsg);
    }
  }

  private FullSyncResponse performSyncForEntity(GitFullSyncEntityInfo entityInfo) {
    final FullSyncServiceGrpc.FullSyncServiceBlockingStub fullSyncServiceBlockingStub =
        fullSyncServiceBlockingStubMap.get(Microservice.fromString(entityInfo.getMicroservice()));
    final YamlGitConfigDTO yamlGitConfigDTO = yamlGitConfigService.get(entityInfo.getProjectIdentifier(),
        entityInfo.getOrgIdentifier(), entityInfo.getAccountIdentifier(), entityInfo.getYamlGitConfigId());
    final FullSyncChangeSet changeSet = getFullSyncChangeSet(entityInfo, yamlGitConfigDTO, entityInfo.getMessageId());
    return GitSyncGrpcClientUtils.retryAndProcessException(fullSyncServiceBlockingStub::performEntitySync, changeSet);
  }

  private FullSyncChangeSet getFullSyncChangeSet(
      GitFullSyncEntityInfo entityInfo, YamlGitConfigDTO yamlGitConfigDTO, String messageId) {
    Map<String, String> logContext = new HashMap<>();
    logContext.put("messageId", messageId);

    return FullSyncChangeSet.newBuilder()
        .setBranchName(yamlGitConfigDTO.getBranch())
        .setEntityDetail(entityDetailRestToProtoMapper.createEntityDetailDTO(entityInfo.getEntityDetail()))
        .setFilePath(entityInfo.getFilePath())
        .setYamlGitConfigIdentifier(yamlGitConfigDTO.getIdentifier())
        .putAllLogContext(logContext)
        .setAccountIdentifier(entityInfo.getAccountIdentifier())
        .setFolderPath(yamlGitConfigDTO.getDefaultRootFolder().getRootFolder())
        .build();
  }
}
