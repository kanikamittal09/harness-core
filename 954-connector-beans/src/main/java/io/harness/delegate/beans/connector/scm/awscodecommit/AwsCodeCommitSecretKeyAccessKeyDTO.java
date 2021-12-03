package io.harness.delegate.beans.connector.scm.awscodecommit;

import static io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorConstants.ACCESS_KEY_AND_SECRET_KEY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.validation.OneOfField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CI)
@Value
@Builder
@JsonTypeName(ACCESS_KEY_AND_SECRET_KEY)
@ApiModel("AwsCodeCommitSecretKeyAccessKeyDTO")
@OneOfField(fields = {"accessKey", "accessKeyRef"})
@Schema(name = "AwsCodeCommitSecretKeyAccessKey",
    description = "This contains details of the AWS Code Commit secret references")
public class AwsCodeCommitSecretKeyAccessKeyDTO implements AwsCodeCommitHttpsCredentialsSpecDTO {
  String accessKey;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData accessKeyRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData secretKeyRef;
}
