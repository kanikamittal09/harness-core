/**
 *
 */

package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Graph.Node;
import software.wings.sm.ExecutionStatus;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * The Class WorkflowExecution.
 *
 * @author Rishi
 */
@Entity(value = "workflowExecutions", noClassnameStored = true)
public class WorkflowExecution extends Base {
  @Indexed private String workflowId;

  private String stateMachineId;
  @Indexed private String envId;
  private String appName;
  private String envName;
  private EnvironmentType envType;
  @Indexed private WorkflowType workflowType;
  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;
  @Transient private Graph graph;
  @Transient private List<String> expandedGroupIds;

  @Transient private Graph.Node executionNode;

  private ErrorStrategy errorStrategy;

  private String name;
  private int total;
  private CountsByStatuses breakdown;

  private ExecutionArgs executionArgs;
  private List<ElementExecutionSummary> serviceExecutionSummaries;
  private LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> statusInstanceBreakdownMap;

  private Long startTs;
  private Long endTs;

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets workflow id.
   *
   * @return the workflow id
   */
  public String getWorkflowId() {
    return workflowId;
  }

  /**
   * Sets workflow id.
   *
   * @param workflowId the workflow id
   */
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  /**
   * Gets state machine id.
   *
   * @return the state machine id
   */
  public String getStateMachineId() {
    return stateMachineId;
  }

  /**
   * Sets state machine id.
   *
   * @param stateMachineId the state machine id
   */
  public void setStateMachineId(String stateMachineId) {
    this.stateMachineId = stateMachineId;
  }

  /**
   * Gets workflow type.
   *
   * @return the workflow type
   */
  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  /**
   * Sets workflow type.
   *
   * @param workflowType the workflow type
   */
  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
  }

  /**
   * Gets graph.
   *
   * @return the graph
   */
  public Graph getGraph() {
    return graph;
  }

  /**
   * Sets graph.
   *
   * @param graph the graph
   */
  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public ExecutionStatus getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets expanded group ids.
   *
   * @return the expanded group ids
   */
  public List<String> getExpandedGroupIds() {
    return expandedGroupIds;
  }

  /**
   * Sets expanded group ids.
   *
   * @param expandedGroupIds the expanded group ids
   */
  public void setExpandedGroupIds(List<String> expandedGroupIds) {
    this.expandedGroupIds = expandedGroupIds;
  }

  /**
   * Getter for property 'total'.
   *
   * @return Value for property 'total'.
   */
  public int getTotal() {
    return total;
  }

  /**
   * Setter for property 'total'.
   *
   * @param total Value to set for property 'total'.
   */
  public void setTotal(int total) {
    this.total = total;
  }

  /**
   * Gets breakdown.
   *
   * @return the breakdown
   */
  public CountsByStatuses getBreakdown() {
    return breakdown;
  }

  /**
   * Sets breakdown.
   *
   * @param breakdown the breakdown
   */
  public void setBreakdown(CountsByStatuses breakdown) {
    this.breakdown = breakdown;
  }

  /**
   * Gets execution args.
   *
   * @return the execution args
   */
  public ExecutionArgs getExecutionArgs() {
    return executionArgs;
  }

  /**
   * Sets execution args.
   *
   * @param executionArgs the execution args
   */
  public void setExecutionArgs(ExecutionArgs executionArgs) {
    this.executionArgs = executionArgs;
  }

  /**
   * Gets service execution summaries.
   *
   * @return the service execution summaries
   */
  public List<ElementExecutionSummary> getServiceExecutionSummaries() {
    return serviceExecutionSummaries;
  }

  /**
   * Sets service execution summaries.
   *
   * @param serviceExecutionSummaries the service execution summaries
   */
  public void setServiceExecutionSummaries(List<ElementExecutionSummary> serviceExecutionSummaries) {
    this.serviceExecutionSummaries = serviceExecutionSummaries;
  }

  /**
   * Gets status instance breakdown map.
   *
   * @return the status instance breakdown map
   */
  public LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> getStatusInstanceBreakdownMap() {
    return statusInstanceBreakdownMap;
  }

  /**
   * Sets status instance breakdown map.
   *
   * @param statusInstanceBreakdownMap the status instance breakdown map
   */
  public void setStatusInstanceBreakdownMap(
      LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> statusInstanceBreakdownMap) {
    this.statusInstanceBreakdownMap = statusInstanceBreakdownMap;
  }

  /**
   * Gets execution node.
   *
   * @return the execution node
   */
  public Node getExecutionNode() {
    return executionNode;
  }

  /**
   * Sets execution node.
   *
   * @param executionNode the execution node
   */
  public void setExecutionNode(Node executionNode) {
    this.executionNode = executionNode;
  }

  /**
   * Is running status boolean.
   *
   * @return the boolean
   */
  public boolean isRunningStatus() {
    return status != null
        && (status == ExecutionStatus.NEW || status == ExecutionStatus.STARTING || status == ExecutionStatus.RUNNING
               || status == ExecutionStatus.ABORTING);
  }

  /**
   * Is failed status boolean.
   *
   * @return the boolean
   */
  public boolean isFailedStatus() {
    return status != null
        && (status == ExecutionStatus.FAILED || status == ExecutionStatus.ABORTED || status == ExecutionStatus.ERROR);
  }

  /**
   * Is paused status boolean.
   *
   * @return the boolean
   */
  public boolean isPausedStatus() {
    return status != null && (status == ExecutionStatus.PAUSED || status == ExecutionStatus.WAITING);
  }

  /**
   * Gets start ts.
   *
   * @return the start ts
   */
  public Long getStartTs() {
    return startTs;
  }

  /**
   * Sets start ts.
   *
   * @param startTs the start ts
   */
  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  /**
   * Gets end ts.
   *
   * @return the end ts
   */
  public Long getEndTs() {
    return endTs;
  }

  /**
   * Sets end ts.
   *
   * @param endTs the end ts
   */
  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  /**
   * Gets error strategy.
   *
   * @return the error strategy
   */
  public ErrorStrategy getErrorStrategy() {
    return errorStrategy;
  }

  /**
   * Sets error strategy.
   *
   * @param errorStrategy the error strategy
   */
  public void setErrorStrategy(ErrorStrategy errorStrategy) {
    this.errorStrategy = errorStrategy;
  }

  /**
   * Gets app name.
   *
   * @return the app name
   */
  public String getAppName() {
    return appName;
  }

  /**
   * Sets app name.
   *
   * @param appName the app name
   */
  public void setAppName(String appName) {
    this.appName = appName;
  }

  /**
   * Gets env name.
   *
   * @return the env name
   */
  public String getEnvName() {
    return envName;
  }

  /**
   * Sets env name.
   *
   * @param envName the env name
   */
  public void setEnvName(String envName) {
    this.envName = envName;
  }

  public EnvironmentType getEnvType() {
    return envType;
  }

  public void setEnvType(EnvironmentType envType) {
    this.envType = envType;
  }

  public static final class WorkflowExecutionBuilder {
    private String workflowId;
    private String stateMachineId;
    private String envId;
    private String appName;
    private String envName;
    private EnvironmentType envType;
    private WorkflowType workflowType;
    private ExecutionStatus status = ExecutionStatus.NEW;
    private Graph graph;
    private List<String> expandedGroupIds;
    private Node executionNode;
    private ErrorStrategy errorStrategy;
    private String name;
    private int total;
    private CountsByStatuses breakdown;
    private ExecutionArgs executionArgs;
    private List<ElementExecutionSummary> serviceExecutionSummaries;
    private String uuid;
    private LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> statusInstanceBreakdownMap;
    private String appId;
    private EmbeddedUser createdBy;
    private Long startTs;
    private Long endTs;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private WorkflowExecutionBuilder() {}

    public static WorkflowExecutionBuilder aWorkflowExecution() {
      return new WorkflowExecutionBuilder();
    }

    public WorkflowExecutionBuilder withWorkflowId(String workflowId) {
      this.workflowId = workflowId;
      return this;
    }

    public WorkflowExecutionBuilder withStateMachineId(String stateMachineId) {
      this.stateMachineId = stateMachineId;
      return this;
    }

    public WorkflowExecutionBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public WorkflowExecutionBuilder withAppName(String appName) {
      this.appName = appName;
      return this;
    }

    public WorkflowExecutionBuilder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    public WorkflowExecutionBuilder withEnvType(EnvironmentType envType) {
      this.envType = envType;
      return this;
    }

    public WorkflowExecutionBuilder withWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    public WorkflowExecutionBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public WorkflowExecutionBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public WorkflowExecutionBuilder withExpandedGroupIds(List<String> expandedGroupIds) {
      this.expandedGroupIds = expandedGroupIds;
      return this;
    }

    public WorkflowExecutionBuilder withExecutionNode(Node executionNode) {
      this.executionNode = executionNode;
      return this;
    }

    public WorkflowExecutionBuilder withErrorStrategy(ErrorStrategy errorStrategy) {
      this.errorStrategy = errorStrategy;
      return this;
    }

    public WorkflowExecutionBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public WorkflowExecutionBuilder withTotal(int total) {
      this.total = total;
      return this;
    }

    public WorkflowExecutionBuilder withBreakdown(CountsByStatuses breakdown) {
      this.breakdown = breakdown;
      return this;
    }

    public WorkflowExecutionBuilder withExecutionArgs(ExecutionArgs executionArgs) {
      this.executionArgs = executionArgs;
      return this;
    }

    public WorkflowExecutionBuilder withServiceExecutionSummaries(
        List<ElementExecutionSummary> serviceExecutionSummaries) {
      this.serviceExecutionSummaries = serviceExecutionSummaries;
      return this;
    }

    public WorkflowExecutionBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public WorkflowExecutionBuilder withStatusInstanceBreakdownMap(
        LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> statusInstanceBreakdownMap) {
      this.statusInstanceBreakdownMap = statusInstanceBreakdownMap;
      return this;
    }

    public WorkflowExecutionBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public WorkflowExecutionBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public WorkflowExecutionBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public WorkflowExecutionBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public WorkflowExecutionBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public WorkflowExecutionBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public WorkflowExecutionBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("workflowId", workflowId)
          .add("stateMachineId", stateMachineId)
          .add("envId", envId)
          .add("appName", appName)
          .add("envName", envName)
          .add("uuid", uuid)
          .add("appId", appId)
          .toString();
    }

    public WorkflowExecutionBuilder but() {
      return aWorkflowExecution()
          .withWorkflowId(workflowId)
          .withStateMachineId(stateMachineId)
          .withEnvId(envId)
          .withAppName(appName)
          .withEnvName(envName)
          .withEnvType(envType)
          .withWorkflowType(workflowType)
          .withStatus(status)
          .withGraph(graph)
          .withExpandedGroupIds(expandedGroupIds)
          .withExecutionNode(executionNode)
          .withErrorStrategy(errorStrategy)
          .withName(name)
          .withTotal(total)
          .withBreakdown(breakdown)
          .withExecutionArgs(executionArgs)
          .withServiceExecutionSummaries(serviceExecutionSummaries)
          .withUuid(uuid)
          .withStatusInstanceBreakdownMap(statusInstanceBreakdownMap)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public WorkflowExecution build() {
      WorkflowExecution workflowExecution = new WorkflowExecution();
      workflowExecution.setWorkflowId(workflowId);
      workflowExecution.setStateMachineId(stateMachineId);
      workflowExecution.setEnvId(envId);
      workflowExecution.setAppName(appName);
      workflowExecution.setEnvName(envName);
      workflowExecution.setEnvType(envType);
      workflowExecution.setWorkflowType(workflowType);
      workflowExecution.setStatus(status);
      workflowExecution.setGraph(graph);
      workflowExecution.setExpandedGroupIds(expandedGroupIds);
      workflowExecution.setExecutionNode(executionNode);
      workflowExecution.setErrorStrategy(errorStrategy);
      workflowExecution.setName(name);
      workflowExecution.setTotal(total);
      workflowExecution.setBreakdown(breakdown);
      workflowExecution.setExecutionArgs(executionArgs);
      workflowExecution.setServiceExecutionSummaries(serviceExecutionSummaries);
      workflowExecution.setUuid(uuid);
      workflowExecution.setStatusInstanceBreakdownMap(statusInstanceBreakdownMap);
      workflowExecution.setAppId(appId);
      workflowExecution.setCreatedBy(createdBy);
      workflowExecution.setStartTs(startTs);
      workflowExecution.setEndTs(endTs);
      workflowExecution.setCreatedAt(createdAt);
      workflowExecution.setLastUpdatedBy(lastUpdatedBy);
      workflowExecution.setLastUpdatedAt(lastUpdatedAt);
      return workflowExecution;
    }
  }
}
