package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Optional;

/**
 * Created by brett on 2/27/17
 */
@JsonTypeName("GCP_KUBERNETES")
@Data
@EqualsAndHashCode(callSuper = true)
public class GcpKubernetesInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Cluster Name", required = true) private String clusterName;
  @Attributes(title = "Namespace") private String namespace;

  /**
   * Instantiates a new Infrastructure mapping.
   */
  public GcpKubernetesInfrastructureMapping() {
    super(InfrastructureMappingType.GCP_KUBERNETES.name());
  }

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  @SchemaIgnore
  @Override
  public String getDisplayName() {
    return String.format("%s (GCP/Kubernetes::%s) %s", this.getClusterName(),
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        Optional.ofNullable(this.getNamespace()).orElse("default"));
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String clusterName;
    private String namespace;
    private String computeProviderSettingId;
    private String envId;
    private String serviceTemplateId;
    private String serviceId;
    private String computeProviderType;
    private String deploymentType;
    private String computeProviderName;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A GCP kubernetes infrastructure mapping builder.
     *
     * @return the builder
     */
    public static Builder aGcpKubernetesInfrastructureMapping() {
      return new Builder();
    }

    /**
     * With cluster name builder.
     *
     * @param clusterName the cluster name
     * @return the builder
     */
    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withNamespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    /**
     * With compute provider setting id builder.
     *
     * @param computeProviderSettingId the compute provider setting id
     * @return the builder
     */
    public Builder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With service template id builder.
     *
     * @param serviceTemplateId the service template id
     * @return the builder
     */
    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
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
     * With compute provider type builder.
     *
     * @param computeProviderType the compute provider type
     * @return the builder
     */
    public Builder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    /**
     * With deployment type builder.
     *
     * @param deploymentType the deployment type
     * @return the builder
     */
    public Builder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    /**
     * With compute provider name name builder.
     *
     * @param computeProviderName the display name
     * @return the builder
     */
    public Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aGcpKubernetesInfrastructureMapping()
          .withClusterName(clusterName)
          .withNamespace(namespace)
          .withComputeProviderSettingId(computeProviderSettingId)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withServiceId(serviceId)
          .withComputeProviderType(computeProviderType)
          .withDeploymentType(deploymentType)
          .withComputeProviderName(computeProviderName)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build kubernetes infrastructure mapping.
     *
     * @return the kubernetes infrastructure mapping
     */
    public GcpKubernetesInfrastructureMapping build() {
      GcpKubernetesInfrastructureMapping gcpKubernetesInfrastructureMapping = new GcpKubernetesInfrastructureMapping();
      gcpKubernetesInfrastructureMapping.setClusterName(clusterName);
      gcpKubernetesInfrastructureMapping.setNamespace(namespace);
      gcpKubernetesInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      gcpKubernetesInfrastructureMapping.setEnvId(envId);
      gcpKubernetesInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      gcpKubernetesInfrastructureMapping.setServiceId(serviceId);
      gcpKubernetesInfrastructureMapping.setComputeProviderType(computeProviderType);
      gcpKubernetesInfrastructureMapping.setDeploymentType(deploymentType);
      gcpKubernetesInfrastructureMapping.setComputeProviderName(computeProviderName);
      gcpKubernetesInfrastructureMapping.setUuid(uuid);
      gcpKubernetesInfrastructureMapping.setAppId(appId);
      gcpKubernetesInfrastructureMapping.setCreatedBy(createdBy);
      gcpKubernetesInfrastructureMapping.setCreatedAt(createdAt);
      gcpKubernetesInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      gcpKubernetesInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      return gcpKubernetesInfrastructureMapping;
    }
  }
}
