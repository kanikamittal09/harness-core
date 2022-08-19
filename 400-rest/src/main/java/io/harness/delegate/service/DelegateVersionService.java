/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service;

import static io.harness.beans.FeatureName.USE_IMMUTABLE_DELEGATE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.DelegateType.CE_KUBERNETES;
import static io.harness.delegate.beans.DelegateType.KUBERNETES;
import static io.harness.delegate.beans.VersionOverrideType.DELEGATE_IMAGE_TAG;
import static io.harness.delegate.beans.VersionOverrideType.DELEGATE_JAR;
import static io.harness.delegate.beans.VersionOverrideType.UPGRADER_IMAGE_TAG;
import static io.harness.delegate.beans.VersionOverrideType.WATCHER_JAR;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import io.harness.delegate.beans.VersionOverride;
import io.harness.delegate.beans.VersionOverride.VersionOverrideKeys;
import io.harness.delegate.beans.VersionOverrideType;
import io.harness.delegate.service.intfc.DelegateRingService;
import io.harness.ff.FeatureFlagService;
import io.harness.network.Http;
import io.harness.persistence.HPersistence;

import software.wings.app.MainConfiguration;
import software.wings.service.impl.infra.InfraDownloadService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DelegateVersionService {
  public static final String DEFAULT_DELEGATE_IMAGE_TAG = "harness/delegate:latest";
  public static final String DEFAULT_UPGRADER_IMAGE_TAG = "harness/upgrader:latest";
  private final DelegateRingService delegateRingService;
  private final InfraDownloadService infraDownloadService;
  private final FeatureFlagService featureFlagService;
  private final MainConfiguration mainConfiguration;
  private final HPersistence persistence;

  public String getDelegateImageTag(final String accountId, final String delegateType) {
    final VersionOverride versionOverride = getVersionOverride(accountId, DELEGATE_IMAGE_TAG);
    if (versionOverride != null && isNotBlank(versionOverride.getVersion())) {
      return versionOverride.getVersion();
    }

    final String ringImage = delegateRingService.getDelegateImageTag(accountId);
    if (isImmutableDelegate(accountId, delegateType) && isNotBlank(ringImage)) {
      return ringImage;
    }

    final String managerConfigImage = mainConfiguration.getPortal().getDelegateDockerImage();
    if (isNotBlank(managerConfigImage)) {
      return managerConfigImage;
    }
    return DEFAULT_DELEGATE_IMAGE_TAG;
  }

  /**
   * Separate function to generate delegate image tag for helm delegates in ng. Keeping a separate function for
   * helm delegates because we don't want to pass igNgDelegate parameter as part of above function.
   * @param accountId
   * @return
   */
  public String getImmutableDelegateImageTag(final String accountId) {
    final VersionOverride versionOverride = getVersionOverride(accountId, DELEGATE_IMAGE_TAG);
    if (versionOverride != null && isNotBlank(versionOverride.getVersion())) {
      return versionOverride.getVersion();
    }

    final String ringImage = delegateRingService.getDelegateImageTag(accountId);
    if (isNotBlank(ringImage)) {
      return ringImage;
    }
    throw new IllegalStateException("No immutable delegate image tag found in ring");
  }

  public String getUpgraderImageTag(final String accountId, final String delegateType) {
    final VersionOverride versionOverride = getVersionOverride(accountId, UPGRADER_IMAGE_TAG);
    if (versionOverride != null && isNotBlank(versionOverride.getVersion())) {
      return versionOverride.getVersion();
    }

    final String ringImage = delegateRingService.getUpgraderImageTag(accountId);
    if (isImmutableDelegate(accountId, delegateType) && isNotBlank(ringImage)) {
      return ringImage;
    }

    if (isNotBlank(mainConfiguration.getPortal().getUpgraderDockerImage())) {
      return mainConfiguration.getPortal().getUpgraderDockerImage();
    }
    return DEFAULT_UPGRADER_IMAGE_TAG;
  }

  public List<String> getDelegateJarVersions(final String accountId) {
    final VersionOverride versionOverride = getVersionOverride(accountId, DELEGATE_JAR);
    if (versionOverride != null && isNotBlank(versionOverride.getVersion())) {
      return singletonList(versionOverride.getVersion());
    }

    final List<String> ringVersion = delegateRingService.getDelegateVersions(accountId);
    if (!CollectionUtils.isEmpty(ringVersion)) {
      return ringVersion;
    }

    return Collections.emptyList();
  }

  public List<String> getDelegateJarVersions(final String ringName, final String accountId) {
    if (isNotEmpty(accountId)) {
      final VersionOverride versionOverride = getVersionOverride(accountId, DELEGATE_JAR);
      if (versionOverride != null && isNotBlank(versionOverride.getVersion())) {
        return Collections.singletonList(versionOverride.getVersion());
      }
    }

    final List<String> ringVersion = delegateRingService.getDelegateVersionsForRing(ringName);
    if (!CollectionUtils.isEmpty(ringVersion)) {
      return ringVersion;
    }
    return Collections.emptyList();
  }

  public String getWatcherJarVersions(final String accountId) {
    final VersionOverride versionOverride = getVersionOverride(accountId, WATCHER_JAR);
    if (versionOverride != null && isNotBlank(versionOverride.getVersion())) {
      return versionOverride.getVersion();
    }
    final String watcherVerionFromRing = delegateRingService.getWatcherVersions(accountId);
    if (isNotEmpty(watcherVerionFromRing)) {
      return watcherVerionFromRing;
    }
    // Get watcher version from gcp.
    final String watcherMetadataUrl = infraDownloadService.getCdnWatcherMetaDataFileUrl();
    try {
      final String watcherMetadata = Http.getResponseStringFromUrl(watcherMetadataUrl, 10, 10);
      return substringBefore(watcherMetadata, " ").trim();
    } catch (Exception ex) {
      log.error("Unable to fetch watcher version from {} ", watcherMetadataUrl, ex);
      throw new IllegalStateException("Unable to fetch watcher version");
    }
  }

  private VersionOverride getVersionOverride(final String accountId, final VersionOverrideType overrideType) {
    return persistence.createQuery(VersionOverride.class)
        .filter(VersionOverrideKeys.accountId, accountId)
        .filter(VersionOverrideKeys.overrideType, overrideType)
        .get();
  }

  private boolean isImmutableDelegate(final String accountId, final String delegateType) {
    // helm delegate only supports immutable delegate hence bypassing FF for helm delegates.
    return featureFlagService.isEnabled(USE_IMMUTABLE_DELEGATE, accountId)
        && (KUBERNETES.equals(delegateType) || CE_KUBERNETES.equals(delegateType));
  }
}
