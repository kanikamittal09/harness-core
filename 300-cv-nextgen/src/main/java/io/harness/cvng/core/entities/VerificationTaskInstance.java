package io.harness.cvng.core.entities;

import io.harness.persistence.AccountAccess;

import java.time.Instant;

public interface VerificationTaskInstance extends VerificationTaskIdAware, AccountAccess {
  Instant getStartTime();
  Instant getEndTime();
}
