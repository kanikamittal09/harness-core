package software.wings.beans;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.SourceRepoProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class GitConfigYaml extends SourceRepoProviderYaml {
  private String branch;
  private String reference;
  private boolean keyAuth;
  private String sshKeyName;
  private String description;
  private String authorName;
  private String authorEmailId;
  private String commitMessage;
  private GitConfig.UrlType urlType;

  @Builder
  public GitConfigYaml(String type, String harnessApiVersion, String url, String username, String password,
      String branch, String reference, UsageRestrictionYaml usageRestrictions, boolean keyAuth, String sshKeyName,
      String description, String authorName, String authorEmailId, String commitMessage, GitConfig.UrlType urlType) {
    super(type, harnessApiVersion, url, username, password, usageRestrictions);
    this.branch = branch;
    this.reference = reference;
    this.keyAuth = keyAuth;
    this.sshKeyName = sshKeyName;
    this.description = description;
    this.authorName = authorName;
    this.authorEmailId = authorEmailId;
    this.commitMessage = commitMessage;
    this.urlType = urlType;
  }
}
