package software.wings.delegatetasks.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * This is generated from harness specific section of
 * value.yaml read from git repo configured by client
 * Harness
 *   helm:
 *      chart:
 *         url: www.google.com
 *         name: name
 *         version: 1.0.1
 *
 */
@Data
@Builder
@AllArgsConstructor
@OwnedBy(CDP)
public class HelmDeployChartSpec {
  private String url;
  private String name;
  private String version;
}
