/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface OwnedByArtifactStream {
  /**
   * Prune if belongs to artifact stream.
   *
   * @param appId the app id
   * @param artifactStreamId the artifact stream id
   */
  void pruneByArtifactStream(String appId, String artifactStreamId);
}
