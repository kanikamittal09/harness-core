package io.harness.connector.apis.dtos.K8Connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.connector.apis.dtos.connector.ConnectorConfigDTO;
import io.harness.connector.common.kubernetes.KubernetesCredentialType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesClusterConfigDTO implements ConnectorConfigDTO {
  @JsonProperty("type1") KubernetesCredentialType kubernetesCredentialType1;
  @JsonProperty("type") KubernetesCredentialType kubernetesCredentialType;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type1", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = KubernetesDelegateDetailsDTO.class, name = "InheritFromDelegate")
    , @JsonSubTypes.Type(value = KubernetesClusterDetailsDTO.class, name = "ManualConfig")
  })
  KubernetesCredentialDTO config;
}