/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.cvnglog.ExecutionLogDTO;
import io.harness.cvng.beans.cvnglog.TraceableType;
import io.harness.cvng.core.entities.VerificationTaskInstance;
import io.harness.cvng.core.services.api.CVNGLogService;
import io.harness.cvng.core.services.api.ExecutionLogService;
import io.harness.cvng.core.services.api.ExecutionLogger;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.Arrays;
import lombok.Builder;
import lombok.Value;

public class ExecutionLogServiceImpl implements ExecutionLogService {
  @Inject private CVNGLogService cvngLogService;

  @Override
  public ExecutionLogger getLogger(VerificationTaskInstance verificationTaskInstance) {
    return ExecutionLoggerImpl.builder()
        .cvngLogService(cvngLogService)
        .accountId(verificationTaskInstance.getAccountId())
        .verificationTaskId(verificationTaskInstance.getVerificationTaskId())
        .startTime(verificationTaskInstance.getStartTime())
        .endTime(verificationTaskInstance.getEndTime())
        .createdAt(verificationTaskInstance.getCreatedAt())
        .build();
  }

  @Builder
  @Value
  private static class ExecutionLoggerImpl implements ExecutionLogger {
    CVNGLogService cvngLogService;
    String accountId;
    String verificationTaskId;
    Instant startTime;
    Instant endTime;
    long createdAt;

    @Override
    public void log(ExecutionLogDTO.LogLevel logLevel, String message) {
      ExecutionLogDTO executionLogDTO = ExecutionLogDTO.builder()
                                            .accountId(accountId)
                                            .traceableId(verificationTaskId)
                                            .startTime(startTime.toEpochMilli())
                                            .endTime(endTime.toEpochMilli())
                                            .createdAt(createdAt)
                                            .traceableType(TraceableType.VERIFICATION_TASK)
                                            .log(message)
                                            .logLevel(logLevel)
                                            .build();
      cvngLogService.save(Arrays.asList(executionLogDTO));
    }
  }
}
