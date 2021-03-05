package software.wings.beans.defaults;

import software.wings.beans.NameValuePairYaml;
import software.wings.yaml.BaseEntityYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DefaultsYaml extends BaseEntityYaml {
  private List<NameValuePairYaml> defaults;

  @Builder
  public DefaultsYaml(String type, String harnessApiVersion, List<NameValuePairYaml> defaults) {
    super(type, harnessApiVersion);
    this.defaults = defaults;
  }
}
