package software.wings.yaml;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseYamlWithType extends BaseYaml {
  /**
   * There are several types at different levels.
   * For example, at the root level, we have APP, SERVICE, WORKFLOW etc.
   * Each root type can have sub-types, for example WORKFLOW can have Canary, Multi-Service,
   * etc which are all modeled as different yamls.
   * The type reflects the sub type.
   */
  private String type;

  protected BaseYamlWithType() {}

  protected BaseYamlWithType(String type) {
    this.type = type;
  }
}
