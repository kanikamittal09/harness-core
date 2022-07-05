/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.jira.resources.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.jira.*;

import java.util.List;

@OwnedBy(CDC)
public interface JiraResourceService {
  boolean validateCredentials(IdentifierRef jiraConnectorRef, String orgId, String projectId);
  List<JiraProjectBasicNG> getProjects(IdentifierRef jiraConnectorRef, String orgId, String projectId);
  List<JiraStatusNG> getStatuses(
      IdentifierRef jiraConnectorRef, String orgId, String projectId, String projectKey, String issueType);
  JiraIssueCreateMetadataNG getIssueCreateMetadata(IdentifierRef jiraConnectorRef, String orgId, String projectId,
      String projectKey, String issueType, String expand, boolean fetchStatus, boolean ignoreComment);
  JiraIssueUpdateMetadataNG getIssueUpdateMetadata(
      IdentifierRef jiraConnectorRef, String orgId, String projectId, String issueKey);
  List<JiraUserData> searchUser(
      String connectorId, String accountId, String appId, long timeoutMillis, String userQuery, String offset);
}
