/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.azure.appservice.deployment.context;

import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureServiceCallBack;

import lombok.Builder;
import lombok.NonNull;

public class SlotStatusVerifierContext extends StatusVerifierContext {
  @Builder
  public SlotStatusVerifierContext(@NonNull LogCallback logCallback, @NonNull String slotName,
      @NonNull AzureWebClient azureWebClient, @NonNull AzureWebClientContext azureWebClientContext,
      AzureServiceCallBack restCallBack) {
    super(logCallback, slotName, azureWebClient, azureWebClientContext, restCallBack);
  }
}