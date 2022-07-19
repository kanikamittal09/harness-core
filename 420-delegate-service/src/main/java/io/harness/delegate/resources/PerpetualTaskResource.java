package io.harness.delegate.resources;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.HeartbeatRequest;
import io.harness.perpetualtask.PerpetualTaskAssignDetails;
import io.harness.perpetualtask.PerpetualTaskContextResponse;
import io.harness.perpetualtask.PerpetualTaskListResponse;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.security.annotations.DelegateAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.dropwizard.jersey.protobuf.ProtocolBufferMediaType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Api("/agent/delegates/perpetual-task")
@Path("/agent/delegates/perpetual-task")
@Produces(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
@Slf4j
@OwnedBy(DEL)
public class PerpetualTaskResource {
  @Inject private PerpetualTaskService perpetualTaskService;

  @GET
  @Path("/list")
  @Timed
  @ExceptionMetered
  @DelegateAuth
  @ApiOperation(value = "Get list of perpetual task assigned to delegate", nickname = "perpetualTaskList")
  public Response perpetualTaskList(
      @QueryParam("delegateId") String delegateId, @QueryParam("accountId") String accountId) {
    List<PerpetualTaskAssignDetails> perpetualTaskAssignDetails =
        perpetualTaskService.listAssignedTasks(delegateId, accountId);
    PerpetualTaskListResponse perpetualTaskListResponse =
        PerpetualTaskListResponse.newBuilder().addAllPerpetualTaskAssignDetails(perpetualTaskAssignDetails).build();
    return Response.ok(perpetualTaskListResponse).build();
  }

  @GET
  @Path("/context")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  @ApiOperation(value = "Get perpetual task context for given perpetual task", nickname = "perpetualTaskContext")
  public Response perpetualTaskContext(@QueryParam("taskId") String taskId) {
    PerpetualTaskContextResponse response =
        PerpetualTaskContextResponse.newBuilder()
            .setPerpetualTaskContext(perpetualTaskService.perpetualTaskContext(taskId))
            .build();
    return Response.ok(response).build();
  }

  @PUT
  @Path("/heartbeat")
  @Timed
  @ExceptionMetered
  @DelegateAuth
  @ApiOperation(value = "Heartbeat recording", nickname = "heartbeat")
  public Response heartbeat(HeartbeatRequest heartbeatRequest) {
    PerpetualTaskResponse perpetualTaskResponse = PerpetualTaskResponse.builder()
                                                      .responseMessage(heartbeatRequest.getResponseMessage())
                                                      .responseCode(heartbeatRequest.getResponseCode())
                                                      .build();
    long heartbeatMillis = HTimestamps.toInstant(heartbeatRequest.getHeartbeatTimestamp()).toEpochMilli();
    perpetualTaskService.triggerCallback(heartbeatRequest.getId(), heartbeatMillis, perpetualTaskResponse);
    return Response.ok().build();
  }
}
