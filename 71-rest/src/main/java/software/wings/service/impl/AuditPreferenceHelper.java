package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.beans.AccountAuditFilter;
import software.wings.beans.Application;
import software.wings.beans.ApplicationAuditFilter;
import software.wings.beans.AuditPreference;
import software.wings.beans.Preference;
import software.wings.service.intfc.AppService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class AuditPreferenceHelper {
  public static final String ENTITY_AUDIT_RECORDS = "entityAuditRecords";
  @Inject AppService appService;

  public Preference parseJsonIntoPreference(String preferenceJson) {
    ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    try {
      return mapper.readValue(preferenceJson, AuditPreference.class);
    } catch (IOException e) {
      String errorMsg = "Failed to Deserialize json into Audit Filter";
      throw new WingsException(ErrorCode.GENERAL_ERROR, errorMsg, USER).addParam("message", errorMsg);
    }
  }

  public PageRequest<AuditHeader> generatePageRequestFromAuditPreference(
      AuditPreference auditPreference, String offset, String limit) {
    PageRequest<AuditHeader> pageRequest = new PageRequest<>();
    SearchFilter searchFilter = null;

    if (hasOnlyAccountLevelResourceCriteria(auditPreference)) {
      // Only AccountLevel Resource Criteria are mentioned
      searchFilter = generatePageRequestWithOnlyAccLevelResourceCriteria(auditPreference);

    } else if (hasOnlyApplicationLevelResourceCriteria(auditPreference)) {
      // Only Application Level Resource Criteria are mentioned
      searchFilter = generatePageRequestWithOnlyAppLevelResourceCriteria(auditPreference);

    } else if (hasAccountAndApplicationLevelCriteria(auditPreference)) {
      // Both Application Level And Account Level Resource Criteria are mentioned
      SearchFilter accountSearchFilter = generatePageRequestWithOnlyAccLevelResourceCriteria(auditPreference);
      SearchFilter applicationSearchFilter = generatePageRequestWithOnlyAppLevelResourceCriteria(auditPreference);

      searchFilter = SearchFilter.builder()
                         .fieldName("query")
                         .op(Operator.OR)
                         .fieldValues(new Object[] {applicationSearchFilter, accountSearchFilter})
                         .build();
    } else {
      logger.info("No Account level or App level criteria were mentioned.");
      if (isNotEmpty(auditPreference.getOperationTypes())) {
        List<String> opTypes = auditPreference.getOperationTypes();
        String[] opTypeArr = opTypes.toArray(new String[opTypes.size()]);
        searchFilter = buildSearchFilter(AuditHeaderKeys.affectedResourceOp, IN, opTypeArr);
      }
    }

    if (searchFilter != null) {
      pageRequest.addFilter(searchFilter);
    }

    addTopLevelAuditPreferenceCriteria(pageRequest, auditPreference, offset, limit);
    return pageRequest;
  }

  /**
   * This method uses ApplicationResourceFilter to create SearchFilters those will be set into PageRequest.
   * AuditPreference.opTypes() are combined with entityRecord searchFilters
   *
   * e.g. ApplicationResourceFilter {resourceIds=[], resourceTypes=[SERVICE], appIds=[APP1]}  &
   * AuditPreference.opTypes() =[UPDATE] we will look for AuditHeaders having UPDATE operation for any SERVICE under app
   * APP1
   */

  private SearchFilter generatePageRequestWithOnlyAppLevelResourceCriteria(AuditPreference auditPreference) {
    ApplicationAuditFilter applicationAuditFilter = auditPreference.getApplicationAuditFilter();
    boolean filterHasOperationTypes = isNotEmpty(auditPreference.getOperationTypes());
    SearchFilter searchFilter = null;

    if (appFilterContainsResourceIds(applicationAuditFilter)) {
      // ResourceIds are non-empty, means fetch specific audit records. Ignore any ResourceTypes if mentioned.
      List<String> resourceIds = applicationAuditFilter.getResourceIds();
      String[] resourceIdArr = resourceIds.toArray(new String[resourceIds.size()]);

      if (!filterHasOperationTypes) {
        searchFilter = buildSearchFilter(AuditHeaderKeys.affectedResourceId, IN, resourceIdArr);
      } else {
        searchFilter = generateElementMatchFilterWithOperationType(
            auditPreference, Arrays.asList(buildSearchFilter("affectedResourceId", IN, resourceIdArr)));
      }
    } else if (appFilterContainsOnlyAppIds(applicationAuditFilter) || appFilterHasNoData(applicationAuditFilter)) {
      // Filter contains appIds but no resourceTypes, so fetch auditRecords given appIds, OR
      // filter has no data, means fetch data for all apps belonging to this acc.
      // e.g.{appIds=[app1, app2], resourceTypes =[], resourceIds = []} || {appIds=[],resourceTypes=[],resourceIds=[]}
      List<String> appIds = appFilterHasNoData(applicationAuditFilter)
          ? appService.getAppIdsByAccountId(auditPreference.getAccountId())
          : applicationAuditFilter.getAppIds();
      String[] appIdsArr = appIds.toArray(new String[appIds.size()]);

      if (isNotEmpty(appIds)) {
        if (!filterHasOperationTypes) {
          searchFilter = buildSearchFilter(AuditHeaderKeys.appIdEntityRecord, IN, appIdsArr);
        } else {
          searchFilter = generateElementMatchFilterWithOperationType(
              auditPreference, Arrays.asList(buildSearchFilter("appId", IN, appIdsArr)));
        }
      }
    } else if (appFilterContainsOnlyResourceTypes(applicationAuditFilter)) {
      // Filter contains resourceTypes but no appIds , so fetch all auditRecords for these resources.
      // e.g. {appIds=[], resourceTypes =["SERVICE", "WORKFLOW"], resourceIds = []}
      List<String> resourceTypes = applicationAuditFilter.getResourceTypes();
      String[] resourceTypesArr = resourceTypes.toArray(new String[resourceTypes.size()]);
      if (!filterHasOperationTypes) {
        searchFilter = buildSearchFilter(AuditHeaderKeys.affectedResourceType, IN, resourceTypesArr);
      } else {
        searchFilter = generateElementMatchFilterWithOperationType(
            auditPreference, Arrays.asList(buildSearchFilter("affectedResourceType", IN, resourceTypesArr)));
      }
    } else {
      // Filter contains  both appId and resourceTypes data ,
      // e.g. {appIds=[APP1, APP2], resourceTypes =[SERVICE, WORKFLOW], resourceIds = []}
      // i.e. fetch all auditRecords for Services and Workflows under applications app1 and app2.

      List<String> resourceTypes = applicationAuditFilter.getResourceTypes();
      String[] resourceTypesArr = resourceTypes.toArray(new String[resourceTypes.size()]);
      List<String> appIds = applicationAuditFilter.getAppIds();
      String[] appIdsArr = appIds.toArray(new String[appIds.size()]);

      searchFilter = generateElementMatchFilterWithOperationType(auditPreference,
          Arrays.asList(SearchFilter.builder()
                            .fieldName("affectedResourceType")
                            .op(IN)
                            .fieldValues(resourceTypes.toArray(new String[resourceTypes.size()]))
                            .build(),
              SearchFilter.builder()
                  .fieldName("appId")
                  .op(IN)
                  .fieldValues(appIds.toArray(new String[appIds.size()]))
                  .build()));
    }

    return searchFilter;
  }

  /**
   * This method uses AccountResourceFilter to create SearchFilters those will be set into PageRequest.
   * AuditPreference.opTypes() are combined with entityRecord searchFilters
   *
   * e.g. AccountResourceFilter {resourceIds=[], resourceTypes=[CLOUD_PROVIDER]}  &  AuditPreference.opTypes() =[CREATE]
   * we will look for AuditHeaders having CREATE operation for any CLOUD_PROVIDER
   */
  private SearchFilter generatePageRequestWithOnlyAccLevelResourceCriteria(AuditPreference auditPreference) {
    AccountAuditFilter accountAuditFilter = auditPreference.getAccountAuditFilter();
    boolean filterHasOperationTypes = isNotEmpty(auditPreference.getOperationTypes());
    SearchFilter searchFilter = null;

    if (accountFilterContainsResourceIds(accountAuditFilter)) {
      // ResourceIds are non-empty, means fetch specific audit records. Ignore any ResourceTypes if mentioned.
      // e.g. {ResourceIds=[ID1,ID2]}
      List<String> resourceIds = accountAuditFilter.getResourceIds();
      String[] resourceIdArr = resourceIds.toArray(new String[resourceIds.size()]);

      if (!filterHasOperationTypes) {
        searchFilter = buildSearchFilter(AuditHeaderKeys.affectedResourceId, IN, resourceIdArr);
      } else {
        searchFilter = generateElementMatchFilterWithOperationType(
            auditPreference, Arrays.asList(buildSearchFilter("affectedResourceId", IN, resourceIdArr)));
      }
    } else if (accountFilterHasNoData(accountAuditFilter)) {
      // ResourceTypes and ResourceIds are empty, means fetch all acc level audit records. use "GLOBAL_APP_ID" for that.
      // e.g. {ResourceTypes=[], ResourceIds=[]}
      if (!filterHasOperationTypes) {
        searchFilter =
            buildSearchFilter(AuditHeaderKeys.appIdEntityRecord, EQ, new String[] {Application.GLOBAL_APP_ID});
      } else {
        searchFilter = generateElementMatchFilterWithOperationType(
            auditPreference, Arrays.asList(buildSearchFilter("appId", EQ, new String[] {Application.GLOBAL_APP_ID})));
      }

    } else {
      // Only ResourceTypes are mentioned.
      // e.g. {ResourceTypes=[CLOUD_PROVIDER, SOURCE_REPO_PROVIDER], ResourceIds=[]}
      List<String> resourceTypes = accountAuditFilter.getResourceTypes();
      String[] resourceTypeArr = resourceTypes.toArray(new String[resourceTypes.size()]);
      if (!filterHasOperationTypes) {
        searchFilter = buildSearchFilter(AuditHeaderKeys.affectedResourceType, IN, resourceTypeArr);
      } else {
        searchFilter = generateElementMatchFilterWithOperationType(
            auditPreference, Arrays.asList(buildSearchFilter("affectedResourceType", IN, resourceTypeArr)));
      }
    }
    return searchFilter;
  }

  private SearchFilter generateElementMatchFilterWithOperationType(
      AuditPreference auditPreference, List<SearchFilter> searchFilterList) {
    PageRequest pageRequest = aPageRequest().build();
    searchFilterList.forEach(searchFilter -> pageRequest.addFilter(searchFilter));

    if (isNotEmpty(auditPreference.getOperationTypes())) {
      pageRequest.addFilter(
          generateSearchFilterForOpTypeEleMatch(auditPreference.getOperationTypes(), "affectedResourceOperation"));
    }

    return SearchFilter.builder()
        .fieldName(ENTITY_AUDIT_RECORDS)
        .op(Operator.ELEMENT_MATCH)
        .fieldValues(new Object[] {pageRequest})
        .build();
  }

  private SearchFilter generateSearchFilterForOpTypeEleMatch(List<String> opTypes, String affectedResourceOperation) {
    return buildSearchFilter(affectedResourceOperation, IN, opTypes.toArray(new String[opTypes.size()]));
  }

  private SearchFilter buildSearchFilter(String filedName, Operator operator, Object[] fieldValues) {
    return SearchFilter.builder().fieldName(filedName).op(operator).fieldValues(fieldValues).build();
  }

  private void addTopLevelAuditPreferenceCriteria(
      PageRequest<AuditHeader> auditHeaderPageRequest, AuditPreference auditPreference, String offset, String limit) {
    auditHeaderPageRequest.setOffset(offset);
    auditHeaderPageRequest.setLimit(limit);
    auditHeaderPageRequest.addFilter(AuditHeaderKeys.accountId, EQ, auditPreference.getAccountId());

    setCreatedAtFilter(auditHeaderPageRequest, auditPreference);

    if (isNotEmpty(auditPreference.getCreatedByUserIds())) {
      List<String> userIds = auditPreference.getCreatedByUserIds();
      auditHeaderPageRequest.addFilter(AuditHeaderKeys.createdById, IN, userIds.toArray(new String[userIds.size()]));
    }

    auditHeaderPageRequest.addOrder(AuditHeaderKeys.createdAt, OrderType.DESC);
  }

  private void setCreatedAtFilter(PageRequest<AuditHeader> auditHeaderPageRequest, AuditPreference auditPreference) {
    Integer lastNDays = auditPreference.getLastNDays();
    if (lastNDays != null && lastNDays.intValue() > 0) {
      long startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(lastNDays.longValue());
      auditHeaderPageRequest.addFilter(AuditHeaderKeys.createdAt, Operator.GE, startTime);

    } else if (isNotBlank(auditPreference.getStartTime()) && isNotBlank(auditPreference.getEndTime())) {
      long startTime = Long.parseLong(auditPreference.getStartTime());
      long endTime = Long.parseLong(auditPreference.getEndTime());

      auditHeaderPageRequest.addFilter(AuditHeaderKeys.createdAt, Operator.GE, startTime);
      auditHeaderPageRequest.addFilter(AuditHeaderKeys.createdAt, Operator.LT, endTime);
    }
  }

  private boolean hasOnlyAccountLevelResourceCriteria(AuditPreference auditPreference) {
    return hasAccountLevelFilterCriteria(auditPreference) && !hasApplicationLevelFilterCriteria(auditPreference);
  }

  private boolean hasOnlyApplicationLevelResourceCriteria(AuditPreference auditPreference) {
    return !hasAccountLevelFilterCriteria(auditPreference) && hasApplicationLevelFilterCriteria(auditPreference);
  }

  private boolean hasAccountLevelFilterCriteria(AuditPreference auditPreference) {
    return auditPreference.isIncludeAccountLevelResources() && auditPreference.getAccountAuditFilter() != null;
  }

  private boolean hasApplicationLevelFilterCriteria(AuditPreference auditPreference) {
    return auditPreference.isIncludeAppLevelResources() && auditPreference.getApplicationAuditFilter() != null;
  }

  private boolean hasAccountAndApplicationLevelCriteria(AuditPreference auditPreference) {
    return hasAccountLevelFilterCriteria(auditPreference) && hasApplicationLevelFilterCriteria(auditPreference);
  }

  private boolean appFilterContainsOnlyResourceTypes(ApplicationAuditFilter applicationAuditFilter) {
    return isEmpty(applicationAuditFilter.getAppIds()) && isNotEmpty(applicationAuditFilter.getResourceTypes());
  }

  private boolean appFilterHasNoData(ApplicationAuditFilter applicationAuditFilter) {
    return isEmpty(applicationAuditFilter.getAppIds()) && isEmpty(applicationAuditFilter.getResourceTypes());
  }

  private boolean appFilterContainsOnlyAppIds(ApplicationAuditFilter applicationAuditFilter) {
    return isNotEmpty(applicationAuditFilter.getAppIds()) && isEmpty(applicationAuditFilter.getResourceTypes());
  }

  private boolean appFilterContainsResourceIds(ApplicationAuditFilter applicationAuditFilter) {
    return isNotEmpty(applicationAuditFilter.getResourceIds());
  }

  private boolean accountFilterContainsResourceIds(AccountAuditFilter accountAuditFilter) {
    return isNotEmpty(accountAuditFilter.getResourceIds());
  }

  private boolean accountFilterHasNoData(AccountAuditFilter accountAuditFilter) {
    return isEmpty(accountAuditFilter.getResourceTypes()) && isEmpty(accountAuditFilter.getResourceIds());
  }
}
