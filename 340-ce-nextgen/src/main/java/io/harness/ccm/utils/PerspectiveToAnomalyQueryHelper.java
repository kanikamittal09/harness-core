/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.utils;

import static io.harness.ccm.commons.constants.ViewFieldConstants.AWS_ACCOUNT_FIELD_ID;
import static io.harness.ccm.commons.constants.ViewFieldConstants.AWS_INSTANCE_TYPE_FIELD_ID;
import static io.harness.ccm.commons.constants.ViewFieldConstants.AWS_SERVICE_FIELD_ID;
import static io.harness.ccm.commons.constants.ViewFieldConstants.AWS_USAGE_TYPE_ID;
import static io.harness.ccm.commons.constants.ViewFieldConstants.CLUSTER_NAME_FIELD_ID;
import static io.harness.ccm.commons.constants.ViewFieldConstants.GCP_PRODUCT_FIELD_ID;
import static io.harness.ccm.commons.constants.ViewFieldConstants.GCP_PROJECT_FIELD_ID;
import static io.harness.ccm.commons.constants.ViewFieldConstants.GCP_SKU_DESCRIPTION_FIELD_ID;
import static io.harness.ccm.commons.constants.ViewFieldConstants.NAMESPACE_FIELD_ID;
import static io.harness.ccm.commons.constants.ViewFieldConstants.REGION_FIELD_ID;
import static io.harness.ccm.commons.constants.ViewFieldConstants.WORKLOAD_NAME_FIELD_ID;

import io.harness.ccm.commons.entities.CCMField;
import io.harness.ccm.commons.entities.CCMFilter;
import io.harness.ccm.commons.entities.CCMGroupBy;
import io.harness.ccm.commons.entities.CCMOperator;
import io.harness.ccm.commons.entities.CCMStringFilter;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewIdCondition;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.entities.ViewVisualization;
import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.ViewsQueryBuilder;
import io.harness.exception.InvalidAccessRequestException;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PerspectiveToAnomalyQueryHelper {
  @Inject ViewsQueryBuilder viewsQueryBuilder;

  public List<CCMGroupBy> convertGroupBy(List<QLCEViewGroupBy> groupByList) {
    List<CCMGroupBy> convertedGroupByList = new ArrayList<>();
    groupByList.forEach(groupBy -> {
      if (groupBy.getEntityGroupBy() != null) {
        switch (groupBy.getEntityGroupBy().getFieldId()) {
          case CLUSTER_NAME_FIELD_ID:
            convertedGroupByList.add(CCMGroupBy.builder().groupByField(CCMField.CLUSTER_NAME).build());
            break;
          case NAMESPACE_FIELD_ID:
            convertedGroupByList.add(CCMGroupBy.builder().groupByField(CCMField.NAMESPACE).build());
            break;
          case WORKLOAD_NAME_FIELD_ID:
            convertedGroupByList.add(CCMGroupBy.builder().groupByField(CCMField.WORKLOAD).build());
            break;
          case GCP_PROJECT_FIELD_ID:
            convertedGroupByList.add(CCMGroupBy.builder().groupByField(CCMField.GCP_PROJECT).build());
            break;
          case GCP_PRODUCT_FIELD_ID:
            convertedGroupByList.add(CCMGroupBy.builder().groupByField(CCMField.GCP_PRODUCT).build());
            break;
          case GCP_SKU_DESCRIPTION_FIELD_ID:
            convertedGroupByList.add(CCMGroupBy.builder().groupByField(CCMField.GCP_SKU_DESCRIPTION).build());
            break;
          case AWS_ACCOUNT_FIELD_ID:
            convertedGroupByList.add(CCMGroupBy.builder().groupByField(CCMField.AWS_ACCOUNT).build());
            break;
          case AWS_SERVICE_FIELD_ID:
            convertedGroupByList.add(CCMGroupBy.builder().groupByField(CCMField.AWS_SERVICE).build());
            break;
          case AWS_INSTANCE_TYPE_FIELD_ID:
            convertedGroupByList.add(CCMGroupBy.builder().groupByField(CCMField.AWS_INSTANCE_TYPE).build());
            break;
          case AWS_USAGE_TYPE_ID:
            convertedGroupByList.add(CCMGroupBy.builder().groupByField(CCMField.AWS_USAGE_TYPE).build());
            break;
          case REGION_FIELD_ID:
            convertedGroupByList.add(CCMGroupBy.builder().groupByField(CCMField.REGION).build());
            break;
          default:
        }
      }
    });
    return convertedGroupByList;
  }

  public CCMFilter convertFilters(List<QLCEViewFilterWrapper> filters) {
    List<CCMStringFilter> stringFilters = new ArrayList<>();

    filters.forEach(filter -> {
      if (filter.getIdFilter() != null) {
        switch (filter.getIdFilter().getField().getFieldId()) {
          case CLUSTER_NAME_FIELD_ID:
            stringFilters.add(buildStringFilter(
                CCMField.CLUSTER_NAME, filter.getIdFilter().getValues(), filter.getIdFilter().getOperator()));
            break;
          case NAMESPACE_FIELD_ID:
            stringFilters.add(buildStringFilter(
                CCMField.NAMESPACE, filter.getIdFilter().getValues(), filter.getIdFilter().getOperator()));
            break;
          case WORKLOAD_NAME_FIELD_ID:
            stringFilters.add(buildStringFilter(
                CCMField.WORKLOAD, filter.getIdFilter().getValues(), filter.getIdFilter().getOperator()));
            break;
          case GCP_PROJECT_FIELD_ID:
            stringFilters.add(buildStringFilter(
                CCMField.GCP_PROJECT, filter.getIdFilter().getValues(), filter.getIdFilter().getOperator()));
            break;
          case GCP_PRODUCT_FIELD_ID:
            stringFilters.add(buildStringFilter(
                CCMField.GCP_PRODUCT, filter.getIdFilter().getValues(), filter.getIdFilter().getOperator()));
            break;
          case GCP_SKU_DESCRIPTION_FIELD_ID:
            stringFilters.add(buildStringFilter(
                CCMField.GCP_SKU_DESCRIPTION, filter.getIdFilter().getValues(), filter.getIdFilter().getOperator()));
            break;
          case AWS_ACCOUNT_FIELD_ID:
            stringFilters.add(buildStringFilter(
                CCMField.AWS_ACCOUNT, filter.getIdFilter().getValues(), filter.getIdFilter().getOperator()));
            break;
          case AWS_SERVICE_FIELD_ID:
            stringFilters.add(buildStringFilter(
                CCMField.AWS_SERVICE, filter.getIdFilter().getValues(), filter.getIdFilter().getOperator()));
            break;
          case AWS_INSTANCE_TYPE_FIELD_ID:
            stringFilters.add(buildStringFilter(
                CCMField.AWS_INSTANCE_TYPE, filter.getIdFilter().getValues(), filter.getIdFilter().getOperator()));
            break;
          case AWS_USAGE_TYPE_ID:
            stringFilters.add(buildStringFilter(
                CCMField.AWS_USAGE_TYPE, filter.getIdFilter().getValues(), filter.getIdFilter().getOperator()));
            break;
          case REGION_FIELD_ID:
            stringFilters.add(buildStringFilter(
                CCMField.REGION, filter.getIdFilter().getValues(), filter.getIdFilter().getOperator()));
            break;
          default:
        }
      }
    });

    return CCMFilter.builder().stringFilters(stringFilters).build();
  }

  public List<QLCEViewGroupBy> getPerspectiveDefaultGroupBy(CEView view) {
    List<QLCEViewGroupBy> defaultGroupBy = new ArrayList<>();
    if (view.getViewVisualization() != null) {
      ViewVisualization viewVisualization = view.getViewVisualization();
      ViewField defaultGroupByField = viewVisualization.getGroupBy();
      defaultGroupBy.add(QLCEViewGroupBy.builder()
                             .entityGroupBy(QLCEViewFieldInput.builder()
                                                .fieldId(defaultGroupByField.getFieldId())
                                                .fieldName(defaultGroupByField.getFieldName())
                                                .identifier(defaultGroupByField.getIdentifier())
                                                .identifierName(defaultGroupByField.getIdentifierName())
                                                .build())
                             .build());
    }
    return defaultGroupBy;
  }

  public List<QLCEViewFilterWrapper> getPerspectiveDefaultFilters(CEView view) {
    List<QLCEViewFilterWrapper> defaultFilters = new ArrayList<>();
    List<ViewRule> viewRules = view.getViewRules();

    for (ViewRule rule : viewRules) {
      rule.getViewConditions().forEach(condition
          -> defaultFilters.add(QLCEViewFilterWrapper.builder()
                                    .idFilter(viewsQueryBuilder.mapConditionToFilter((ViewIdCondition) condition))
                                    .build()));
    }
    return defaultFilters;
  }

  private CCMStringFilter buildStringFilter(CCMField field, String[] values, QLCEViewFilterOperator operator) {
    return CCMStringFilter.builder()
        .field(field)
        .values(Arrays.asList(values))
        .operator(convertFilterOperator(operator))
        .build();
  }

  private CCMOperator convertFilterOperator(QLCEViewFilterOperator operator) {
    switch (operator) {
      case IN:
        return CCMOperator.IN;
      case NOT_IN:
        return CCMOperator.NOT_IN;
      case EQUALS:
        return CCMOperator.EQUALS;
      case LIKE:
        return CCMOperator.LIKE;
      case NULL:
        return CCMOperator.NULL;
      case NOT_NULL:
        return CCMOperator.NOT_NULL;
      default:
        throw new InvalidAccessRequestException("Filter operator not supported");
    }
  }
}
