package software.wings.service.impl.ldap;

import static io.harness.annotations.dev.HarnessModule._360_CG_MANAGER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.helpers.ext.ldap.LdapSearch;
import software.wings.helpers.ext.ldap.LdapUserConfig;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(_360_CG_MANAGER)
public class LdapGetUsersRequest extends AbstractLdapRequest {
  LdapSearch ldapSearch;
  String groupBaseDn;
  @Builder.Default Boolean useRecursiveGroupMembershipSearch = Boolean.TRUE;

  public LdapGetUsersRequest(@NotNull final LdapUserConfig ldapUserConfig, @NotNull final LdapSearch ldapSearch,
      int responseTimeoutInSeconds, String groupBaseDn, Boolean useRecursiveGroupMembershipSearch) {
    super(ldapUserConfig, responseTimeoutInSeconds);
    this.ldapSearch = ldapSearch;
    this.groupBaseDn = groupBaseDn;
    this.useRecursiveGroupMembershipSearch = useRecursiveGroupMembershipSearch;
  }
}
