package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;
import software.wings.stencils.EnumData;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeName("AWS")
public class AwsInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Restrictions")
  @EnumData(enumDataProvider = AwsInfrastructureRestrictionProvider.class)
  private String restrictionType;
  @Attributes(title = "Expression") private String restrictionExpression;

  /**
   * Instantiates a new Aws infrastructure mapping.
   */
  public AwsInfrastructureMapping() {
    super(SettingVariableTypes.AWS.name());
  }

  /**
   * Gets restriction type.
   *
   * @return the restriction type
   */
  public String getRestrictionType() {
    return restrictionType;
  }

  /**
   * Sets restriction type.
   *
   * @param restrictionType the restriction type
   */
  public void setRestrictionType(String restrictionType) {
    this.restrictionType = restrictionType;
  }

  /**
   * Gets restriction expression.
   *
   * @return the restriction expression
   */
  public String getRestrictionExpression() {
    return restrictionExpression;
  }

  /**
   * Sets restriction expression.
   *
   * @param restrictionExpression the restriction expression
   */
  public void setRestrictionExpression(String restrictionExpression) {
    this.restrictionExpression = restrictionExpression;
  }

  /**
   * The type Aws infrastructure restriction provider.
   */
  public static class AwsInfrastructureRestrictionProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, String... params) {
      return Arrays.stream(RestrictionType.values())
          .collect(Collectors.toMap(RestrictionType::name, RestrictionType::getDisplayName));
    }
  }

  /**
   * The enum Restriction type.
   */
  public enum RestrictionType {
    /**
     * None restriction type.
     */
    NONE("None"), /**
                   * Instance restriction type.
                   */
    INSTANCE("By specific instances"), /**
                                        * Custom restriction type.
                                        */
    CUSTOM("By zone/tags etc");

    RestrictionType(String displayName) {
      this.displayName = displayName;
    }

    private String displayName;

    /**
     * Gets display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
      return displayName;
    }

    /**
     * Sets display name.
     *
     * @param displayName the display name
     */
    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String computeProviderSettingId;
    private String envId;
    private String serviceTemplateId;
    private String computeProviderType;
    private String restrictionType;
    private String hostConnectionAttrs;
    private String restrictionExpression;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * An aws infrastructure mapping builder.
     *
     * @return the builder
     */
    public static Builder anAwsInfrastructureMapping() {
      return new Builder();
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
     * With restriction type builder.
     *
     * @param restrictionType the restriction type
     * @return the builder
     */
    public Builder withRestrictionType(String restrictionType) {
      this.restrictionType = restrictionType;
      return this;
    }

    /**
     * With host connection attrs builder.
     *
     * @param hostConnectionAttrs the host connection attrs
     * @return the builder
     */
    public Builder withHostConnectionAttrs(String hostConnectionAttrs) {
      this.hostConnectionAttrs = hostConnectionAttrs;
      return this;
    }

    /**
     * With restriction expression builder.
     *
     * @param restrictionExpression the restriction expression
     * @return the builder
     */
    public Builder withRestrictionExpression(String restrictionExpression) {
      this.restrictionExpression = restrictionExpression;
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
      return anAwsInfrastructureMapping()
          .withComputeProviderSettingId(computeProviderSettingId)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withComputeProviderType(computeProviderType)
          .withRestrictionType(restrictionType)
          .withHostConnectionAttrs(hostConnectionAttrs)
          .withRestrictionExpression(restrictionExpression)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build aws infrastructure mapping.
     *
     * @return the aws infrastructure mapping
     */
    public AwsInfrastructureMapping build() {
      AwsInfrastructureMapping awsInfrastructureMapping = new AwsInfrastructureMapping();
      awsInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      awsInfrastructureMapping.setEnvId(envId);
      awsInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      awsInfrastructureMapping.setComputeProviderType(computeProviderType);
      awsInfrastructureMapping.setRestrictionType(restrictionType);
      awsInfrastructureMapping.setHostConnectionAttrs(hostConnectionAttrs);
      awsInfrastructureMapping.setRestrictionExpression(restrictionExpression);
      awsInfrastructureMapping.setUuid(uuid);
      awsInfrastructureMapping.setAppId(appId);
      awsInfrastructureMapping.setCreatedBy(createdBy);
      awsInfrastructureMapping.setCreatedAt(createdAt);
      awsInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      awsInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      return awsInfrastructureMapping;
    }
  }
}
