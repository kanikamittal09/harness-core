package software.wings.graphql.schema.mutation.cloudProvider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLUpdateGcpCloudProviderInput {
  private RequestField<String> name;
  private RequestField<String> serviceAccountKeySecretId;
  private RequestField<List<String>> delegateSelector;
  private RequestField<Boolean> useDelegate;
  private RequestField<Boolean> skipValidation;
}
