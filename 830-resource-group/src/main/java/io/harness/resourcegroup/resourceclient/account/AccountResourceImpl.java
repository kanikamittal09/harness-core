package io.harness.resourcegroup.resourceclient.account;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.remote.client.RestClientUtils.getResponse;
import static io.harness.resourcegroup.beans.ValidatorType.STATIC;

import static java.util.stream.Collectors.toList;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.account.AccountEntityChangeDTO;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.resourcegroup.beans.ValidatorType;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceInfo;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
@Slf4j
public class AccountResourceImpl implements Resource {
  AccountClient accountClient;

  @Override
  public List<Boolean> validate(List<String> resourceIds, Scope scope) {
    List<AccountDTO> accounts = getResponse(accountClient.getAccountDTOs(resourceIds));
    Set<String> validResourceIds = accounts.stream().map(AccountDTO::getIdentifier).collect(Collectors.toSet());
    return resourceIds.stream()
        .map(resourceId -> validResourceIds.contains(resourceId) && scope.getAccountIdentifier().equals(resourceId))
        .collect(toList());
  }

  @Override
  public EnumSet<ValidatorType> getSelectorKind() {
    return EnumSet.of(STATIC);
  }

  @Override
  public String getType() {
    return "ACCOUNT";
  }

  @Override
  public Set<ScopeLevel> getValidScopeLevels() {
    return Collections.emptySet();
  }

  @Override
  public Optional<String> getEventFrameworkEntityType() {
    return Optional.of(EventsFrameworkMetadataConstants.ACCOUNT_ENTITY);
  }

  @Override
  public ResourceInfo getResourceInfoFromEvent(Message message) {
    AccountEntityChangeDTO accountEntityChangeDTO;
    try {
      accountEntityChangeDTO = AccountEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking AccountEntityChangeDTO for key {}", message.getId(), e);
      return null;
    }
    if (Objects.isNull(accountEntityChangeDTO)) {
      return null;
    }
    return ResourceInfo.builder()
        .accountIdentifier(accountEntityChangeDTO.getAccountId())
        .resourceType(getType())
        .resourceIdentifier(accountEntityChangeDTO.getAccountId())
        .build();
  }
}
