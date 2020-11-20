package io.harness.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SecretUpdateData;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.MorphiaIterator;

import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
public interface SecretsDao {
  String ID_KEY = "_id";

  Optional<EncryptedData> getSecretById(@NotEmpty String accountId, @NotEmpty String secretId);

  Optional<EncryptedData> getSecretByName(@NotEmpty String accountId, @NotEmpty String secretName);

  Optional<EncryptedData> getSecretByKeyOrPath(
      @NotEmpty String accountId, @NotNull EncryptionType encryptionType, String key, String path);

  String saveSecret(@NotNull EncryptedData encryptedData);

  EncryptedData updateSecret(@NotNull SecretUpdateData secretUpdateData, EncryptedRecord updatedEncryptedData);

  EncryptedData migrateSecret(
      @NotEmpty String accountId, @NotEmpty String secretId, @NotNull EncryptedRecord encryptedRecord);

  boolean deleteSecret(@NotEmpty String accountId, @NotEmpty String secretId);

  PageResponse<EncryptedData> listSecrets(PageRequest<EncryptedData> pageRequest);

  MorphiaIterator<EncryptedData, EncryptedData> listSecretsBySecretManager(
      @NotEmpty String accountId, @NotEmpty String secretManagerId, boolean shouldIncludeSecretManagerSecrets);

  MorphiaIterator<EncryptedData, EncryptedData> listSecretsBySecretIds(
      @NotEmpty String accountId, @NotNull Set<String> secretIds);
}
