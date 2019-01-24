package software.wings.service.intfc;

import software.wings.beans.User;
import software.wings.beans.security.restrictions.RestrictionsSummary;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserPermissionInfo;
import software.wings.settings.RestrictionsAndAppEnvMap;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;
import software.wings.settings.UsageRestrictionsReferenceSummary;

import java.util.Map;
import java.util.Set;

/**
 * Usage restrictions related service apis. Account level entities (cloud providers, connectors, secrets) has concept of
 * usage restrictions. The user can restrict entities to certain apps and envs.
 *
 * @author rktummala on 06/05/2018
 */
public interface UsageRestrictionsService {
  /**
   *
   * @param accountId
   * @param appEnvRestrictions
   * @return
   */
  Map<String, Set<String>> getAppEnvMap(String accountId, Set<AppEnvRestriction> appEnvRestrictions, Action action);

  /**
   * Derive the user restrictions from user permissions.
   * @param accountId account Id
   * @param action
   * @return Usage restrictions
   */
  UsageRestrictions getUsageRestrictionsFromUserPermissions(
      String accountId, UserPermissionInfo userPermissionInfo, User user, Action action);

  /**
   * Check if the user has access to an entity from the given context.
   * Access is determined from Usage restrictions, permissions and context.
   *
   * @param accountId account
   *@param appIdFromRequest current app context
   * @param envIdFromRequest current env context
   * @param entityUsageRestrictions
   * @param restrictionsFromUserPermissions
   * @param appEnvMapFromPermissions       @return boolean if the user needs to be provided access or not
   */
  boolean hasAccess(String accountId, boolean isAccountAdmin, String appIdFromRequest, String envIdFromRequest,
      UsageRestrictions entityUsageRestrictions, UsageRestrictions restrictionsFromUserPermissions,
      Map<String, Set<String>> appEnvMapFromPermissions);

  /**
   * Lists all the applications and environments that the user has update permissions on.
   * The update permissions could be on any of the env based permission types. (For example, permission types ENV /
   * WORKFLOW / PIPELINE)
   * @param accountId account id
   * @return Restrictions summary that has app and env info.
   */
  RestrictionsSummary listAppsWithEnvUpdatePermissions(String accountId);

  UsageRestrictions getUsageRestrictionsFromJson(String usageRestrictionsString);

  /**
   * Gets the default usage restrictions based on user permissions and the current app env context.
   * @param accountId account id
   * @param appId app id
   * @param envId env id
   * @return usage restrictions
   */
  UsageRestrictions getDefaultUsageRestrictions(String accountId, String appId, String envId);

  /**
   * Checks if the user can update / delete entity based on the user permissions and restrictions set on the entity.
   * @param accountId account id
   * @param entityUsageRestrictions entity usage restrictions
   * @param restrictionsFromUserPermissions restrictions from user permissions
   * @return boolean
   */
  boolean userHasPermissionsToChangeEntity(
      String accountId, UsageRestrictions entityUsageRestrictions, UsageRestrictions restrictionsFromUserPermissions);

  /**
   * Checks if the user can update / delete entity based on the user permissions and restrictions set on the entity.
   * @param accountId account id
   * @param entityUsageRestrictions entity usage restrictions
   * @return boolean
   */
  boolean userHasPermissionsToChangeEntity(String accountId, UsageRestrictions entityUsageRestrictions);

  /**
   * Checks if user can update an entity
   * @param oldUsageRestrictions old usage restrictions
   * @param newUsageRestrictions new usage restrictions
   * @param accountId account id
   */
  void validateUsageRestrictionsOnEntityUpdate(
      String accountId, UsageRestrictions oldUsageRestrictions, UsageRestrictions newUsageRestrictions);

  boolean isAccountAdmin(String accountId);

  /**
   * Checks if user can create an entity.
   * @param newUsageRestrictions new usage restrictions in case of update
   * @param accountId account id
   */
  void validateUsageRestrictionsOnEntitySave(String accountId, UsageRestrictions newUsageRestrictions);

  boolean hasNoRestrictions(UsageRestrictions usageRestrictions);

  /**
   * Constructs the app env map from user permissions
   * @param accountId account id
   * @param userPermissionInfo user permission info
   * @param action action
   * @return App env map
   */
  Map<String, Set<String>> getAppEnvMapFromUserPermissions(
      String accountId, UserPermissionInfo userPermissionInfo, Action action);

  /**
   * Gets the usage restrictions and app env map from the user permission cache
   * @param accountId account id
   * @param action action
   * @return
   */
  RestrictionsAndAppEnvMap getRestrictionsAndAppEnvMapFromCache(String accountId, Action action);

  boolean isEditable(String accountId, String entityId, String entityType);

  /**
   * Get the summary of all usage restriction references to the specified application.
   */
  UsageRestrictionsReferenceSummary getReferenceSummaryForApp(String accountId, String appId);

  /**
   * Get the summary of all usage restriction references to the specified environment.
   */
  UsageRestrictionsReferenceSummary getReferenceSummaryForEnv(String accountId, String envId);

  /**
   * Purge all the usage restriction references to application/environments that no longer exists.
   */
  int purgeDanglingAppEnvReferences(String accountId);

  /**
   * Remove all references to an application or an environment in the usage restrictions. This operation is usually
   * performed before an application is deleted to prevent dangling references to deleted applications.
   */
  int removeAppEnvReferences(String accountId, String appId, String envId);
}
