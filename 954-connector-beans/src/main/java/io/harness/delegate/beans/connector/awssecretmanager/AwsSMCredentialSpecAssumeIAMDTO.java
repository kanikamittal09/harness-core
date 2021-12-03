package io.harness.delegate.beans.connector.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AwsSecretManagerConstants.ASSUME_IAM_ROLE)
@ApiModel("AwsSMCredentialSpecAssumeIAM")
@Schema(name = "AwsSMCredentialSpecAssumeIAM", description = "This contains the credential spec of AWS SM for IAM role")
public class AwsSMCredentialSpecAssumeIAMDTO implements AwsSecretManagerCredentialSpecDTO {}
