package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.NexusArtifactStream.Builder.aNexusArtifactStream;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;
import software.wings.beans.EmbeddedUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by srinivas on 3/31/17.
 */
@JsonTypeName("NEXUS")
public class NexusArtifactStream extends ArtifactStream {
  private String jobname;
  private String groupId;
  private String imageName;
  private List<String> artifactPaths;

  /**
   * Instantiates a new Nexus artifact stream.
   */
  public NexusArtifactStream() {
    super(ArtifactStreamType.NEXUS.name());
    super.setAutoApproveForProduction(true);
  }

  public String getJobname() {
    return jobname;
  }

  public void setJobname(String jobname) {
    this.jobname = jobname;
  }

  public String getImageName() {
    return imageName;
  }

  public String getArtifactDisplayName(String buildNo) {
    if (StringUtils.isBlank(getImageName())) {
      return String.format("%s_%s_%s", getSourceName(), buildNo, getDateFormat().format(new Date()));
    }
    return String.format("%s_%s_%s", getJobname() + "/" + getImageName(), buildNo, getDateFormat().format(new Date()));
  }

  public String getArtifactStreamType() {
    return super.getArtifactStreamType();
  }

  public String getSettingId() {
    return super.getSettingId();
  }

  public boolean getAutoApproveForProduction() {
    return super.isAutoApproveForProduction();
  }

  /**
   * Gets artifact paths.
   *
   * @return the artifact paths
   */
  public List<String> getArtifactPaths() {
    return artifactPaths;
  }

  /**
   * Sets artifact paths.
   *
   * @param artifactPaths the artifact paths
   */
  public void setArtifactPaths(List<String> artifactPaths) {
    this.artifactPaths = artifactPaths;
  }

  /**
   * @return groupId
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Set Group Id
   */
  public void setGroupId(String groupId) {
    this.groupId = groupId;
    this.imageName = groupId;
  }

  /**
   * Sets image name.
   *
   * @param imageName the image name
   */
  public void setImageName(String imageName) {
    this.imageName = imageName;
  }

  @Override
  @SchemaIgnore
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withJobName(jobname)
        .withGroupId(groupId)
        .withImageName(imageName)
        .withArtifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .build();
  }

  @Override
  public ArtifactStream clone() {
    return aNexusArtifactStream()
        .withAppId(getAppId())
        .withSourceName(getSourceName())
        .withSettingId(getSettingId())
        .withAutoApproveForProduction(getAutoApproveForProduction())
        .withJobname(getJobname())
        .withGroupId(getGroupId())
        .withMetadataOnly(getMetadataOnly())
        .withArtifactPaths(getArtifactPaths())
        .build();
  }

  /**
   * clone and return builder
   * @return
   */
  public Builder deepClone() {
    return aNexusArtifactStream()
        .withJobname(getJobname())
        .withGroupId(getGroupId())
        .withArtifactPaths(getArtifactPaths())
        .withSourceName(getSourceName())
        .withSettingId(getSettingId())
        .withServiceId(getServiceId())
        .withUuid(getUuid())
        .withAppId(getAppId())
        .withCreatedBy(getCreatedBy())
        .withCreatedAt(getCreatedAt())
        .withLastUpdatedBy(getLastUpdatedBy())
        .withLastUpdatedAt(getLastUpdatedAt())
        .withAutoApproveForProduction(isAutoApproveForProduction())
        .withStreamActions(getStreamActions())
        .withMetadataOnly(isMetadataOnly());
  }

  public boolean getMetadataOnly() {
    return super.isMetadataOnly();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String jobname;
    private String groupId;
    private String imageName;
    private List<String> artifactPaths;
    private String sourceName;
    private String settingId;
    private String serviceId;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean autoApproveForProduction = false;
    private boolean metadataOnly = false;
    private List<ArtifactStreamAction> streamActions = new ArrayList<>();

    private Builder() {}

    /**
     * A bamboo artifact stream builder.
     *
     * @return the builder
     */
    public static Builder aNexusArtifactStream() {
      return new Builder();
    }

    /**
     * With jobname builder.
     *
     * @param jobname the jobname
     * @return the builder
     */
    public Builder withJobname(String jobname) {
      this.jobname = jobname;
      return this;
    }

    /**
     * With groupId builder.
     *
     * @param groupId the groupId
     * @return the builder
     */
    public Builder withGroupId(String groupId) {
      this.groupId = groupId;
      return this;
    }

    /**
     * With artifact paths builder.
     *
     * @param artifactPaths the artifact paths
     * @return the builder
     */
    public Builder withArtifactPaths(List<String> artifactPaths) {
      this.artifactPaths = artifactPaths;
      return this;
    }

    /**
     * With source name builder.
     *
     * @param sourceName the source name
     * @return the builder
     */
    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    /**
     * With setting id builder.
     *
     * @param settingId the setting id
     * @return the builder
     */
    public Builder withSettingId(String settingId) {
      this.settingId = settingId;
      return this;
    }

    /**
     * With service id builder.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With auto approve for production builder.
     *
     * @param autoApproveForProduction the auto approve for production
     * @return the builder
     */
    public Builder withAutoApproveForProduction(boolean autoApproveForProduction) {
      this.autoApproveForProduction = autoApproveForProduction;
      return this;
    }

    /**
     * With stream actions builder.
     *
     * @param streamActions the stream actions
     * @return the builder
     */
    public Builder withStreamActions(List<ArtifactStreamAction> streamActions) {
      this.streamActions = streamActions;
      return this;
    }
    /**
     * With MetadataOnly builder.
     */
    public Builder withMetadataOnly(boolean metadataOnly) {
      this.metadataOnly = metadataOnly;
      return this;
    }

    /**
     * With MetadataOnly builder.
     */
    public Builder withImageName(String imageName) {
      this.imageName = imageName;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aNexusArtifactStream()
          .withJobname(jobname)
          .withGroupId(groupId)
          .withArtifactPaths(artifactPaths)
          .withSourceName(sourceName)
          .withSettingId(settingId)
          .withServiceId(serviceId)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withAutoApproveForProduction(autoApproveForProduction)
          .withStreamActions(streamActions)
          .withMetadataOnly(metadataOnly)
          .withImageName(imageName);
    }

    /**
     * Build bamboo artifact stream.
     *
     * @return the bamboo artifact stream
     */
    public NexusArtifactStream build() {
      NexusArtifactStream nexusArtifactStream = new NexusArtifactStream();
      nexusArtifactStream.setJobname(jobname);
      nexusArtifactStream.setGroupId(groupId);
      nexusArtifactStream.setArtifactPaths(artifactPaths);
      nexusArtifactStream.setSourceName(sourceName);
      nexusArtifactStream.setSettingId(settingId);
      nexusArtifactStream.setUuid(uuid);
      nexusArtifactStream.setAppId(appId);
      nexusArtifactStream.setCreatedBy(createdBy);
      nexusArtifactStream.setCreatedAt(createdAt);
      nexusArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      nexusArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      nexusArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      nexusArtifactStream.setMetadataOnly(metadataOnly);
      nexusArtifactStream.setImageName(imageName);
      return nexusArtifactStream;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    private String repositoryName;
    private String groupId;
    private List<String> artifactPaths;
    private String imageName;

    public static final class Builder {
      private String repositoryName;
      private String sourceName;
      private String groupId;
      private String imageName;
      private String settingName;
      private List<String> artifactPaths;
      private boolean autoApproveForProduction = false;
      private String type;
      private boolean metadataOnly = false;

      private Builder() {}

      public static Builder aYaml() {
        return new Builder();
      }

      public Builder withRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
        return this;
      }

      public Builder withSourceName(String sourceName) {
        this.sourceName = sourceName;
        return this;
      }

      public Builder withGroupId(String groupId) {
        this.groupId = groupId;
        return this;
      }

      public Builder withSettingName(String settingName) {
        this.settingName = settingName;
        return this;
      }

      public Builder withArtifactPaths(List<String> artifactPaths) {
        this.artifactPaths = artifactPaths;
        return this;
      }

      public Builder withAutoApproveForProduction(boolean autoApproveForProduction) {
        this.autoApproveForProduction = autoApproveForProduction;
        return this;
      }

      public Builder withType(String type) {
        this.type = type;
        return this;
      }

      public Builder withMetadataOnly(boolean metadataOnly) {
        this.metadataOnly = metadataOnly;
        return this;
      }

      public Builder withImageName(String imageName) {
        this.imageName = imageName;
        return this;
      }

      public Builder but() {
        return aYaml()
            .withRepositoryName(repositoryName)
            .withSourceName(sourceName)
            .withGroupId(groupId)
            .withSettingName(settingName)
            .withArtifactPaths(artifactPaths)
            .withAutoApproveForProduction(autoApproveForProduction)
            .withType(type)
            .withMetadataOnly(metadataOnly);
      }

      public Yaml build() {
        Yaml yaml = new Yaml();
        yaml.setRepositoryName(repositoryName);
        yaml.setSourceName(sourceName);
        yaml.setGroupId(groupId);
        yaml.setSettingName(settingName);
        yaml.setArtifactPaths(artifactPaths);
        yaml.setAutoApproveForProduction(autoApproveForProduction);
        yaml.setType(type);
        yaml.setMetadataOnly(metadataOnly);
        yaml.setImageName(imageName);
        return yaml;
      }
    }
  }
}
