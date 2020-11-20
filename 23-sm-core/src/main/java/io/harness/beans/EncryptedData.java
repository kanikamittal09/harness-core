package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.security.encryption.EncryptionType.CUSTOM;
import static io.harness.security.encryption.EncryptionType.LOCAL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedDataParent.EncryptedDataParentKeys;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.CdSparseIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.ng.core.NGAccess;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.NameAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;
import io.harness.validation.Update;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;
import software.wings.security.ScopedEntity;
import software.wings.security.UsageRestrictions;
import software.wings.settings.SettingVariableTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"encryptionKey", "encryptedValue", "backupEncryptionKey", "backupEncryptedValue"})
@EqualsAndHashCode
@Entity(value = "encryptedRecords", noClassnameStored = true)
@HarnessEntity(exportable = true)
@JsonIgnoreProperties(ignoreUnknown = true)

@NgUniqueIndex(name = "acctNameIdx", fields = { @Field("accountId")
                                                , @Field("name") })
@CdIndex(name = "acctKmsIdx", fields = { @Field("accountId")
                                         , @Field("kmsId") })
@CdSparseIndex(name = "accountIdentifierEntityIdentifierIdx",
    fields = { @Field("ngMetadata.accountIdentifier")
               , @Field("ngMetadata.identifier") })
@CdSparseIndex(name = "accountOrgProjectIdx",
    fields =
    {
      @Field("ngMetadata.accountIdentifier"), @Field("ngMetadata.orgIdentifier"), @Field("ngMetadata.projectIdentifier")
    })
@FieldNameConstants(innerTypeName = "EncryptedDataKeys")
public class EncryptedData
    implements EncryptedRecord, PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
               UpdatedByAware, NameAccess, PersistentRegularIterable, AccountAccess, ScopedEntity, NGAccess {
  public static final String PARENT_ID_KEY =
      String.format("%s.%s", EncryptedDataKeys.parents, EncryptedDataParentKeys.id);
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @FdIndex private long createdAt;
  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @NotEmpty @FdIndex private String name;

  @FdIndex private String encryptionKey;

  private char[] encryptedValue;

  // When 'path' value is set, no actual encryption is needed since it's just referring to a secret in a Secret Manager
  // path.
  @FdIndex private String path;

  @Default private Set<EncryptedDataParams> parameters = new HashSet<>();

  @NotNull private SettingVariableTypes type;

  @Default @FdIndex private Set<EncryptedDataParent> parents = new HashSet<>();

  @NotEmpty private String accountId;

  @Default private boolean enabled = true;

  @NotEmpty private String kmsId;

  @NotNull private EncryptionType encryptionType;

  private long fileSize;

  @Default private List<String> appIds = new ArrayList<>();

  @Default private List<String> serviceIds = new ArrayList<>();

  @Default private List<String> envIds = new ArrayList<>();

  private char[] backupEncryptedValue;

  private String backupEncryptionKey;

  private String backupKmsId;

  private EncryptionType backupEncryptionType;

  private Set<String> serviceVariableIds;

  private Map<String, AtomicInteger> searchTags;

  private boolean scopedToAccount;

  private UsageRestrictions usageRestrictions;

  private boolean inheritScopesFromSM;

  @FdIndex private Long nextMigrationIteration;

  @FdIndex private Long nextAwsToGcpKmsMigrationIteration;

  @SchemaIgnore private boolean base64Encoded;

  @SchemaIgnore @Transient private transient String encryptedBy;

  @SchemaIgnore @Transient private transient int setupUsage;

  @SchemaIgnore @Transient private transient long runTimeUsage;

  @SchemaIgnore @Transient private transient int changeLog;

  @SchemaIgnore @FdIndex private List<String> keywords;

  @JsonIgnore private NGEncryptedDataMetadata ngMetadata;

  @Default private boolean hideFromListing = false;

  public String getKmsId() {
    if (encryptionType == LOCAL) {
      return accountId;
    }
    return kmsId;
  }

  public void addParent(@NotNull EncryptedDataParent encryptedDataParent) {
    parents.add(encryptedDataParent);
  }

  public void removeParent(@NotNull EncryptedDataParent encryptedDataParent) {
    parents.remove(encryptedDataParent);
  }

  public boolean containsParent(@NotNull String id, @NotNull SettingVariableTypes type) {
    return parents.stream().anyMatch(
        encryptedDataParent -> encryptedDataParent.getId().equals(id) && encryptedDataParent.getType() == type);
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (EncryptedDataKeys.nextMigrationIteration.equals(fieldName)) {
      this.nextMigrationIteration = nextIteration;
      return;
    } else if (EncryptedDataKeys.nextAwsToGcpKmsMigrationIteration.equals(fieldName)) {
      this.nextAwsToGcpKmsMigrationIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (EncryptedDataKeys.nextMigrationIteration.equals(fieldName)) {
      return nextMigrationIteration;
    } else if (EncryptedDataKeys.nextAwsToGcpKmsMigrationIteration.equals(fieldName)) {
      return nextAwsToGcpKmsMigrationIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public boolean isInlineSecret() {
    return isEmpty(path) && encryptionType != CUSTOM;
  }

  public boolean isReferencedSecret() {
    return isNotEmpty(path) && encryptionType != CUSTOM;
  }

  public boolean isParameterizedSecret() {
    return encryptionType == CUSTOM;
  }

  public void addApplication(String appId, String appName) {
    if (appIds == null) {
      appIds = new ArrayList<>();
    }
    appIds.add(appId);
    addSearchTag(appName);
  }

  public void removeApplication(String appId, String appName) {
    removeSearchTag(appId, appName, appIds);
  }

  public void addService(String serviceId, String serviceName) {
    if (serviceIds == null) {
      serviceIds = new ArrayList<>();
    }
    serviceIds.add(serviceId);
    addSearchTag(serviceName);
  }

  public void removeService(String serviceId, String serviceName) {
    removeSearchTag(serviceId, serviceName, serviceIds);
  }

  public void addEnvironment(String envId, String environmentName) {
    if (envIds == null) {
      envIds = new ArrayList<>();
    }
    envIds.add(envId);
    addSearchTag(environmentName);
  }

  public void removeEnvironment(String envId, String envName) {
    removeSearchTag(envId, envName, envIds);
  }

  public void addServiceVariable(String serviceVariableId, String serviceVariableName) {
    if (serviceVariableIds == null) {
      serviceVariableIds = new HashSet<>();
    }
    serviceVariableIds.add(serviceVariableId);
    addSearchTag(serviceVariableName);
  }

  public void removeServiceVariable(String serviceVariableId, String serviceVariableName) {
    if (!isEmpty(serviceVariableIds)) {
      serviceVariableIds.remove(serviceVariableId);
    }

    if (!isEmpty(searchTags)) {
      searchTags.remove(serviceVariableName);
    }
  }

  public void addSearchTag(String searchTag) {
    if (searchTags == null) {
      searchTags = new HashMap<>();
    }

    if (searchTags.containsKey(searchTag)) {
      searchTags.get(searchTag).incrementAndGet();
    } else {
      searchTags.put(searchTag, new AtomicInteger(1));
    }

    if (getKeywords() == null) {
      setKeywords(new ArrayList<>());
    }
    if (!getKeywords().contains(searchTag)) {
      getKeywords().add(searchTag);
    }
  }

  public void removeSearchTag(String key, String searchTag, List<String> collection) {
    if (isNotEmpty(collection)) {
      collection.remove(key);
    }

    if (isNotEmpty(searchTags) && searchTags.containsKey(searchTag)
        && searchTags.get(searchTag).decrementAndGet() == 0) {
      searchTags.remove(searchTag);
      if (getKeywords() != null) {
        getKeywords().remove(searchTag);
      }
    }
  }

  public void clearSearchTags() {
    if (!isEmpty(appIds)) {
      appIds.clear();
    }

    if (!isEmpty(serviceIds)) {
      serviceIds.clear();
    }

    if (!isEmpty(envIds)) {
      envIds.clear();
    }

    if (!isEmpty(serviceVariableIds)) {
      serviceVariableIds.clear();
    }

    if (!isEmpty(searchTags)) {
      searchTags.clear();
    }
  }

  @Override
  @JsonIgnore
  public String getIdentifier() {
    return Optional.ofNullable(ngMetadata).map(NGEncryptedDataMetadata::getIdentifier).orElse(null);
  }

  @Override
  @JsonIgnore
  public String getAccountIdentifier() {
    return Optional.ofNullable(ngMetadata).map(NGEncryptedDataMetadata::getAccountIdentifier).orElse(null);
  }

  @Override
  @JsonIgnore
  public String getOrgIdentifier() {
    return Optional.ofNullable(ngMetadata).map(NGEncryptedDataMetadata::getOrgIdentifier).orElse(null);
  }

  @Override
  @JsonIgnore
  public String getProjectIdentifier() {
    return Optional.ofNullable(ngMetadata).map(NGEncryptedDataMetadata::getProjectIdentifier).orElse(null);
  }

  @UtilityClass
  public static final class EncryptedDataKeys {
    // Temporary
    public static final String ID_KEY = "_id";
  }
}
