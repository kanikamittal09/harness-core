package io.harness.ng.core.remote;

import com.google.inject.Inject;
import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.HostValidationResult;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.validator.HostValidatoionService;
import io.harness.security.annotations.NextGenManagerAuth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@Path("/validateSsh")
@Api("/validateSsh")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@Tag(name = "ValidateHost", description = "This contains APIs related to SSH host validation")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
        content =
                {
                        @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
                        , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
                })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
        content =
                {
                        @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
                        , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
                })
@ApiResponses(value =
        {
                @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
                , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
        })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@NextGenManagerAuth
@Slf4j
public class HostValidationResource {

    private final HostValidatoionService hostValidationService;

    @GET
    @ApiOperation(value = "Validate SSH host connectivity", nickname = "validateHost")
    @Operation(operationId = "validateSshHost", summary = "Validates host connectivity using SSH credentials",
            responses =
                    {
                            @io.swagger.v3.oas.annotations.responses.
                                    ApiResponse(responseCode = "default", description = "Returns validation response")
                    })
    public ResponseDTO<HostValidationResult>
    validateSshHost(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
            NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
                   @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
                   @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
                           NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
                   @Parameter (description = "Host name") @QueryParam("host") String hostName,
                   @Parameter(description = "Secret Identifier") @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String secretIdentifier) {


        return ResponseDTO.newResponse(hostValidationService.validateSSHHost(hostName, accountIdentifier, orgIdentifier, projectIdentifier, secretIdentifier));
    }

    @POST
    @Consumes({"application/json"})
    @Path("hosts")
    @ApiOperation(value = "Validate SSH hosts connectivity", nickname = "validateHosts")
    @Operation(operationId = "validateSshHost", summary = "Validates host connectivity using SSH credentials",
            responses =
                    {
                            @io.swagger.v3.oas.annotations.responses.
                                    ApiResponse(responseCode = "default", description = "Returns validation response")
                    })
    public ResponseDTO<List<HostValidationResult>>
    validateSshHost(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
            NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
                    @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
                    @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
                            NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
                    @Parameter(description = "Secret Identifier") @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String secretIdentifier,
                    @RequestBody(required = true, description = "List of host names to validate") @NotNull List<String> hostNames) {

        return ResponseDTO.newResponse(hostValidationService.validateSSHHosts(hostNames, accountIdentifier, orgIdentifier, projectIdentifier, secretIdentifier));
    }

}
