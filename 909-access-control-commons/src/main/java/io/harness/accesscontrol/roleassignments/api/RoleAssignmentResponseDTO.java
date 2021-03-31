package io.harness.accesscontrol.roleassignments.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.scopes.ScopeDTO;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@ApiModel(value = "RoleAssignmentResponse")
public class RoleAssignmentResponseDTO {
  @ApiModelProperty(required = true) RoleAssignmentDTO roleAssignment;
  @ApiModelProperty(required = true) ScopeDTO scope;
  Long createdAt;
  Long lastModifiedAt;
  boolean harnessManaged;
}
