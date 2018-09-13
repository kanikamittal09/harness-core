package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.validation.Create;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.Encrypted;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.encryption.EncryptionInterface;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
@Entity(value = "accounts", noClassnameStored = true)
public class Account extends Base {
  public static final String ACCOUNT_NAME_KEY = "accountName";
  public static final String COMPANY_NAME_KEY = "companyName";

  @Indexed @NotNull private String companyName;

  @Indexed(options = @IndexOptions(unique = true)) @NotNull private String accountName;

  @JsonIgnore @NotNull(groups = Create.class) @Encrypted private String accountKey;

  private String licenseId;

  private List<String> salesContacts;

  @Transient private LicenseInfo licenseInfo;

  @JsonIgnore private byte[] encryptedLicenseInfo;

  @JsonIgnore private EncryptionInterface encryption;
  private boolean twoFactorAdminEnforced;

  private DelegateConfiguration delegateConfiguration;

  private transient Map<String, String> defaults = new HashMap<>();

  public Map<String, String> getDefaults() {
    return defaults;
  }

  public void setDefaults(Map<String, String> defaults) {
    this.defaults = defaults;
  }

  public void setTwoFactorAdminEnforced(boolean twoFactorAdminEnforced) {
    this.twoFactorAdminEnforced = twoFactorAdminEnforced;
  }

  public boolean isTwoFactorAdminEnforced() {
    return twoFactorAdminEnforced;
  }

  /**
   * Default mechanism is USER_PASSWORD
   */
  @JsonIgnore private AuthenticationMechanism authenticationMechanism = AuthenticationMechanism.USER_PASSWORD;

  /**
   * Getter for property 'companyName'.
   *
   * @return Value for property 'companyName'.
   */
  public String getCompanyName() {
    return companyName;
  }

  /**
   * Setter for property 'companyName'.
   *
   * @param companyName Value to set for property 'companyName'.
   */
  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  /**
   * Getter for property 'accountKey'.
   *
   * @return Value for property 'accountKey'.
   */
  public String getAccountKey() {
    return accountKey;
  }

  /**
   * Setter for property 'accountKey'.
   *
   * @param accountKey Value to set for property 'accountKey'.
   */
  public void setAccountKey(String accountKey) {
    this.accountKey = accountKey;
  }

  public String getAccountName() {
    return accountName;
  }
  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  /**
   * Getter for property 'licenseId'.
   *
   * @return Value for property 'licenseId'.
   */
  public String getLicenseId() {
    return licenseId;
  }

  /**
   * Setter for property 'licenseId'.
   *
   * @param licenseId Value to set for property 'licenseId'.
   */
  public void setLicenseId(String licenseId) {
    this.licenseId = licenseId;
  }

  public LicenseInfo getLicenseInfo() {
    return licenseInfo;
  }

  public void setLicenseInfo(LicenseInfo licenseInfo) {
    this.licenseInfo = licenseInfo;
  }

  @JsonIgnore
  public byte[] getEncryptedLicenseInfo() {
    if (encryptedLicenseInfo != null) {
      return Arrays.copyOf(encryptedLicenseInfo, encryptedLicenseInfo.length);
    }

    return null;
  }

  @JsonIgnore
  public void setEncryptedLicenseInfo(byte[] encryptedLicenseInfo) {
    if (encryptedLicenseInfo != null) {
      this.encryptedLicenseInfo = Arrays.copyOf(encryptedLicenseInfo, encryptedLicenseInfo.length);
    }
  }

  public EncryptionInterface getEncryption() {
    return encryption;
  }

  public void setEncryption(EncryptionInterface encryption) {
    this.encryption = encryption;
  }

  public AuthenticationMechanism getAuthenticationMechanism() {
    return authenticationMechanism;
  }

  public void setAuthenticationMechanism(AuthenticationMechanism authenticationMechanism) {
    this.authenticationMechanism = authenticationMechanism;
  }

  public void setDelegateConfiguration(DelegateConfiguration delegateConfiguration) {
    this.delegateConfiguration = delegateConfiguration;
  }

  public DelegateConfiguration getDelegateConfiguration() {
    return delegateConfiguration;
  }

  public List<String> getSalesContacts() {
    return salesContacts;
  }

  public void setSalesContacts(List<String> salesContacts) {
    this.salesContacts = salesContacts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    Account account = (Account) o;

    return accountName != null ? accountName.equals(account.accountName) : account.accountName == null;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (accountName != null ? accountName.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Account{"
        + "companyName='" + companyName + '\'' + ", accountName='" + accountName + '\'' + '}';
  }

  public static final class Builder {
    private String companyName;
    private String accountName;
    private String accountKey;
    private String uuid;
    private String appId = GLOBAL_APP_ID;
    private EncryptionInterface encryption = new SimpleEncryption();
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private AuthenticationMechanism authenticationMechanism;
    private DelegateConfiguration delegateConfiguration;
    private Map<String, String> defaults = new HashMap<>();
    private LicenseInfo licenseInfo;

    private Builder() {}

    public static Builder anAccount() {
      return new Builder();
    }

    public Builder withCompanyName(String companyName) {
      this.companyName = companyName;
      return this;
    }

    public Builder withAccountName(String accountName) {
      this.accountName = accountName;
      return this;
    }

    public Builder withAccountKey(String accountKey) {
      this.accountKey = accountKey;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withEncryption(EncryptionInterface encryption) {
      this.encryption = encryption;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withAuthenticationMechanism(AuthenticationMechanism mechanism) {
      this.authenticationMechanism = mechanism;
      return this;
    }

    public Builder withDelegateConfiguration(DelegateConfiguration delegateConfiguration) {
      this.delegateConfiguration = delegateConfiguration;
      return this;
    }

    public Builder withDefaults(Map<String, String> defaults) {
      this.defaults = defaults;
      return this;
    }

    public Builder withLicenseInfo(LicenseInfo licenseInfo) {
      this.licenseInfo = licenseInfo;
      return this;
    }

    public Builder but() {
      return anAccount()
          .withCompanyName(companyName)
          .withAccountKey(accountKey)
          .withUuid(uuid)
          .withAppId(appId)
          .withEncryption(encryption)
          .withCreatedBy(createdBy)
          .withAccountName(accountName)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withAuthenticationMechanism(authenticationMechanism)
          .withDelegateConfiguration(delegateConfiguration)
          .withDefaults(defaults)
          .withLicenseInfo(licenseInfo);
    }

    public Account build() {
      Account account = new Account();
      account.setCompanyName(companyName);
      account.setAccountName(accountName);
      account.setAccountKey(accountKey);
      account.setUuid(uuid);
      account.setAppId(appId);
      account.setEncryption(encryption);
      account.setCreatedBy(createdBy);
      account.setCreatedAt(createdAt);
      account.setLastUpdatedBy(lastUpdatedBy);
      account.setLastUpdatedAt(lastUpdatedAt);
      account.setAuthenticationMechanism(authenticationMechanism);
      account.setDelegateConfiguration(delegateConfiguration);
      account.setDefaults(defaults);
      account.setLicenseInfo(licenseInfo);
      return account;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseEntityYaml {
    private List<NameValuePair.Yaml> defaults = new ArrayList<>();

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, List<NameValuePair.Yaml> defaults) {
      super(type, harnessApiVersion);
      this.defaults = defaults;
    }
  }
}
