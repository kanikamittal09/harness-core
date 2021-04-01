package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.SecretReference;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "GitBranchInfoKeys")
@OwnedBy(DX)
public class GitBranchInfo {
  String branch;
  String yamlGitConfigId;
  // todo(abhinav): convert to different interfaces depending on how github connector comes out.
  @SecretReference String password;
  // todo(abhinav): See if we need to convert this to .
  String connectorFQN;
  String filePath;
  String accountId;
}
