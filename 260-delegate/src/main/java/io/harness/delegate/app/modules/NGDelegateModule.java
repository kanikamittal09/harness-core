/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.app.modules;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.artifacts.docker.service.DockerRegistryService;
import io.harness.artifacts.docker.service.DockerRegistryServiceImpl;
import io.harness.artifacts.gcr.service.GcrApiService;
import io.harness.artifacts.gcr.service.GcrApiServiceImpl;
import io.harness.delegate.task.artifacts.docker.DockerArtifactTaskHandler;
import io.harness.http.HttpService;
import io.harness.http.HttpServiceImpl;

import com.google.inject.AbstractModule;

@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class NGDelegateModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DockerRegistryService.class).to(DockerRegistryServiceImpl.class);
    bind(GcrApiService.class).to(GcrApiServiceImpl.class);
    bind(HttpService.class).to(HttpServiceImpl.class);
    bind(DockerArtifactTaskHandler.class);
  }
}