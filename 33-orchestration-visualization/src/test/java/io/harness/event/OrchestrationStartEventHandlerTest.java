package io.harness.event;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.execution.events.OrchestrationEventType.ORCHESTRATION_START;
import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationVisualizationTestBase;
import io.harness.ambiance.Ambiance;
import io.harness.beans.OrchestrationGraph;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.pms.execution.Status;
import io.harness.rule.Owner;
import io.harness.service.GraphGenerationService;
import io.harness.testlib.RealMongo;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.TimeUnit;

/**
 * Test class for {@link OrchestrationStartEventHandler}
 */
public class OrchestrationStartEventHandlerTest extends OrchestrationVisualizationTestBase {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private OrchestrationStartEventHandler orchestrationStartEventHandler;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldThrowInvalidRequestException() {
    String planExecutionId = generateUuid();
    OrchestrationEvent event = OrchestrationEvent.builder()
                                   .ambiance(Ambiance.builder().planExecutionId(planExecutionId).build())
                                   .eventType(ORCHESTRATION_START)
                                   .build();

    assertThatThrownBy(() -> orchestrationStartEventHandler.handleEvent(event))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldSaveCachedGraph() {
    PlanExecution planExecution =
        PlanExecution.builder().uuid(generateUuid()).startTs(System.currentTimeMillis()).status(Status.RUNNING).build();
    planExecutionService.save(planExecution);

    OrchestrationEvent event = OrchestrationEvent.builder()
                                   .ambiance(Ambiance.builder().planExecutionId(planExecution.getUuid()).build())
                                   .eventType(ORCHESTRATION_START)
                                   .build();

    orchestrationStartEventHandler.handleEvent(event);

    Awaitility.await().atMost(2, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() -> {
      OrchestrationGraph graphInternal = graphGenerationService.getCachedOrchestrationGraph(planExecution.getUuid());
      return graphInternal != null;
    });

    OrchestrationGraph orchestrationGraph = graphGenerationService.getCachedOrchestrationGraph(planExecution.getUuid());

    assertThat(orchestrationGraph).isNotNull();
    assertThat(orchestrationGraph.getPlanExecutionId()).isEqualTo(planExecution.getUuid());
    assertThat(orchestrationGraph.getStartTs()).isEqualTo(planExecution.getStartTs());
    assertThat(orchestrationGraph.getEndTs()).isNull();
    assertThat(orchestrationGraph.getStatus()).isEqualTo(planExecution.getStatus());
  }
}
