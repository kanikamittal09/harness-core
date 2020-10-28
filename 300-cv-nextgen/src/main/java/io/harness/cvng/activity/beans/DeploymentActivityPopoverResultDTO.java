package io.harness.cvng.activity.beans;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;

@Data
@Builder
public class DeploymentActivityPopoverResultDTO {
  String tag;
  String serviceName;
  DeploymentPopoverSummary preProductionDeploymentSummary;
  DeploymentPopoverSummary productionDeploymentSummary;
  DeploymentPopoverSummary postDeploymentSummary;
  @Value
  @Builder
  public static class DeploymentPopoverSummary {
    int total;
    List<VerificationResult> verificationResults;
  }
  @Value
  @Builder
  public static class VerificationResult {
    String jobName;
    ActivityVerificationStatus status;
    Double riskScore;
    Long remainingTimeMs;
    int progressPercentage;
    Long startTime;
  }
}
