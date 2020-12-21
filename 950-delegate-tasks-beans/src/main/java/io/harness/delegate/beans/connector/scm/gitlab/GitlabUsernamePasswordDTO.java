package io.harness.delegate.beans.connector.scm.gitlab;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GitlabUsernamePassword")
public class GitlabUsernamePasswordDTO implements GitlabHttpCredentialsSpecDTO {
  String username;
  @SecretReference @ApiModelProperty(dataType = "string") SecretRefData usernameRef;
  @NotNull @SecretReference @ApiModelProperty(dataType = "string") SecretRefData passwordRef;
}
