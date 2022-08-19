/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.scmerrorhandling.handlers.bitbucketcloud;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.gitsync.common.scmerrorhandling.handlers.bitbucketcloud.ScmErrorExplanations.INVALID_CONNECTOR_CREDS;
import static io.harness.gitsync.common.scmerrorhandling.handlers.bitbucketcloud.ScmErrorHints.INVALID_CREDENTIALS;
import static io.harness.gitsync.common.scmerrorhandling.handlers.bitbucketcloud.ScmErrorHints.REPO_NOT_FOUND;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.ScmBadRequestException;
import io.harness.exception.ScmConflictException;
import io.harness.exception.ScmUnauthorizedException;
import io.harness.exception.ScmUnexpectedException;
import io.harness.exception.WingsException;
import io.harness.gitsync.common.scmerrorhandling.dtos.ErrorMetadata;
import io.harness.gitsync.common.scmerrorhandling.handlers.ScmApiErrorHandler;
import io.harness.gitsync.common.scmerrorhandling.util.ErrorMessageFormatter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
public class BitbucketUpdateFileScmApiErrorHandler implements ScmApiErrorHandler {
  public static final String UPDATE_FILE_REQUEST_FAILURE =
      "The requested file<FILEPATH> couldn't be updated in Bitbucket. ";
  public static final String UPDATE_FILE_CONFLICT_ERROR_HINT =
      "Please check the input commit id of the requested file<FILEPATH>. It should match with current commit id of the file at head of the branch<BRANCH> in the given Bitbucket repository<REPO>";
  public static final String UPDATE_FILE_CONFLICT_ERROR_EXPLANATION =
      "The input commit id of the requested file<FILEPATH> doesn't match with current commit id of the file at head of the branch<BRANCH> in Bitbucket repository<REPO>, which results in update operation failure.";
  public static final String UPDATE_FILE_BAD_REQUEST_EXPLANATION =
      "The requested branch<BRANCH> does not have push permission.";
  public static final String UPDATE_FILE_BAD_REQUEST_HINT =
      "Please use a pull request to update file<FILEPATH> in this branch<BRANCH>.";

  @Override
  public void handleError(int statusCode, String errorMessage, ErrorMetadata errorMetadata) throws WingsException {
    switch (statusCode) {
      case 401:
      case 403:
        throw NestedExceptionUtils.hintWithExplanationException(
            ErrorMessageFormatter.formatMessage(INVALID_CREDENTIALS, errorMetadata),
            ErrorMessageFormatter.formatMessage(UPDATE_FILE_REQUEST_FAILURE + INVALID_CONNECTOR_CREDS, errorMetadata),
            new ScmUnauthorizedException(errorMessage));
      case 400:
        throw NestedExceptionUtils.hintWithExplanationException(
            ErrorMessageFormatter.formatMessage(UPDATE_FILE_BAD_REQUEST_HINT, errorMetadata),
            ErrorMessageFormatter.formatMessage(
                UPDATE_FILE_REQUEST_FAILURE + UPDATE_FILE_BAD_REQUEST_EXPLANATION, errorMetadata),
            new ScmBadRequestException(errorMessage));
      case 404:
        throw NestedExceptionUtils.hintWithExplanationException(
            ErrorMessageFormatter.formatMessage(REPO_NOT_FOUND, errorMetadata),
            ErrorMessageFormatter.formatMessage(
                UPDATE_FILE_REQUEST_FAILURE + ScmErrorExplanations.REPO_NOT_FOUND, errorMetadata),
            new ScmBadRequestException(errorMessage));
      case 409:
        throw NestedExceptionUtils.hintWithExplanationException(
            ErrorMessageFormatter.formatMessage(UPDATE_FILE_CONFLICT_ERROR_HINT, errorMetadata),
            ErrorMessageFormatter.formatMessage(UPDATE_FILE_CONFLICT_ERROR_EXPLANATION, errorMetadata),
            new ScmConflictException(errorMessage));
      default:
        log.error(String.format("Error while updating bitbucket file: [%s: %s]", statusCode, errorMessage));
        throw new ScmUnexpectedException(errorMessage);
    }
  }
}
