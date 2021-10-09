package io.harness.secretmanagerclient.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.AdditionalMetadata;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;

import software.wings.settings.SettingVariableTypes;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class EncryptedDataMigrationDTO implements EncryptedRecord {
  private String uuid;
  private String name;
  private String path;
  private Set<EncryptedDataParams> parameters;
  private String encryptionKey;
  private char[] encryptedValue;
  private String kmsId;
  private EncryptionType encryptionType;
  private char[] backupEncryptedValue;
  private String backupEncryptionKey;
  private String backupKmsId;
  private EncryptionType backupEncryptionType;
  private boolean base64Encoded;
  private AdditionalMetadata additionalMetadata;
  private SettingVariableTypes type;

  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String identifier;
}
