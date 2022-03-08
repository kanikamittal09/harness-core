package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.VerificationTaskInstance;

public interface ExecutionLogService {
  ExecutionLogger getLogger(VerificationTaskInstance verificationTaskInstance);
}
