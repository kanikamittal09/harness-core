package software.wings.service.impl.analysis;

import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

/**
 * Created by rsingh on 9/26/17.
 */
public class MetricDataAnalysisServiceImpl implements MetricDataAnalysisService {
  private static final Logger logger = LoggerFactory.getLogger(MetricDataAnalysisServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Override
  public boolean saveMetricData(String accountId, String applicationId, List<NewRelicMetricDataRecord> metricData)
      throws IOException {
    logger.debug("inserting " + metricData.size() + " pieces of new relic metrics data");
    wingsPersistence.saveIgnoringDuplicateKeys(metricData);
    logger.debug("inserted " + metricData.size() + " NewRelicMetricDataRecord to persistence layer.");
    return true;
  }

  @Override
  public boolean saveAnalysisRecords(NewRelicMetricAnalysisRecord metricAnalysisRecord) {
    wingsPersistence.delete(wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
                                .field("workflowExecutionId")
                                .equal(metricAnalysisRecord.getWorkflowExecutionId())
                                .field("stateExecutionId")
                                .equal(metricAnalysisRecord.getStateExecutionId()));

    wingsPersistence.save(metricAnalysisRecord);
    logger.debug("inserted NewRelicMetricAnalysisRecord to persistence layer for workflowExecutionId: "
        + metricAnalysisRecord.getWorkflowExecutionId()
        + " StateExecutionInstanceId: " + metricAnalysisRecord.getStateExecutionId());
    return true;
  }

  @Override
  public boolean saveAnalysisRecordsML(TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord) {
    wingsPersistence.delete(wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                                .field("workflowExecutionId")
                                .equal(timeSeriesMLAnalysisRecord.getWorkflowExecutionId())
                                .field("stateExecutionId")
                                .equal(timeSeriesMLAnalysisRecord.getStateExecutionId()));

    wingsPersistence.save(timeSeriesMLAnalysisRecord);
    logger.debug("inserted NewRelicMetricAnalysisRecord to persistence layer for workflowExecutionId: "
        + timeSeriesMLAnalysisRecord.getWorkflowExecutionId()
        + " StateExecutionInstanceId: " + timeSeriesMLAnalysisRecord.getStateExecutionId());
    return true;
  }

  @Override
  public List<NewRelicMetricDataRecord> getRecords(StateType stateType, String workflowExecutionId,
      String stateExecutionId, String workflowId, String serviceId, Set<String> nodes, int analysisMinute) {
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .field("stateType")
                                                .equal(stateType)
                                                .field("workflowId")
                                                .equal(workflowId)
                                                .field("workflowExecutionId")
                                                .equal(workflowExecutionId)
                                                .field("stateExecutionId")
                                                .equal(stateExecutionId)
                                                .field("serviceId")
                                                .equal(serviceId)
                                                .field("host")
                                                .hasAnyOf(nodes)
                                                .field("level")
                                                .notEqual(ClusterLevel.H0)
                                                .field("dataCollectionMinute")
                                                .lessThanOrEq(analysisMinute);
    return query.asList();
  }

  @Override
  public List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(
      StateType stateType, String workflowId, String serviceId, int analysisMinute) {
    final String astSuccessfulWorkflowExecutionIdWithData =
        getLastSuccessfulWorkflowExecutionIdWithData(stateType, workflowId, serviceId);
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .field("stateType")
                                                .equal(stateType)
                                                .field("workflowId")
                                                .equal(workflowId)
                                                .field("workflowExecutionId")
                                                .equal(astSuccessfulWorkflowExecutionIdWithData)
                                                .field("serviceId")
                                                .equal(serviceId)
                                                .field("level")
                                                .notEqual(ClusterLevel.H0)
                                                .field("dataCollectionMinute")
                                                .lessThanOrEq(analysisMinute);
    return query.asList();
  }

  private String getLastSuccessfulWorkflowExecutionIdWithData(
      StateType stateType, String workflowId, String serviceId) {
    List<String> successfulExecutions = getLastSuccessfulWorkflowExecutionIds(workflowId);
    for (String successfulExecution : successfulExecutions) {
      Query<NewRelicMetricDataRecord> lastSuccessfulRecordQuery =
          wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
              .field("stateType")
              .equal(stateType)
              .field("workflowId")
              .equal(workflowId)
              .field("workflowExecutionId")
              .equal(successfulExecution)
              .field("serviceId")
              .equal(serviceId)
              .limit(1);

      List<NewRelicMetricDataRecord> lastSuccessfulRecords = lastSuccessfulRecordQuery.asList();
      if (lastSuccessfulRecords != null && lastSuccessfulRecords.size() > 0) {
        return successfulExecution;
      }
    }
    logger.error("Could not get a successful workflow to find control nodes");
    return null;
  }

  private List<String> getLastSuccessfulWorkflowExecutionIds(String workflowId) {
    final PageRequest<WorkflowExecution> pageRequest = PageRequest.Builder.aPageRequest()
                                                           .addFilter("workflowId", Operator.EQ, workflowId)
                                                           .addFilter("status", Operator.EQ, ExecutionStatus.SUCCESS)
                                                           .addOrder("createdAt", OrderType.DESC)
                                                           .build();

    final PageResponse<WorkflowExecution> workflowExecutions =
        workflowExecutionService.listExecutions(pageRequest, false, true, false, false);
    final List<String> workflowExecutionIds = new ArrayList<>();

    if (workflowExecutions != null) {
      for (WorkflowExecution workflowExecution : workflowExecutions) {
        workflowExecutionIds.add(workflowExecution.getUuid());
      }
    }
    return workflowExecutionIds;
  }

  @Override
  public NewRelicMetricAnalysisRecord getMetricsAnalysis(
      StateType stateType, String stateExecutionId, String workflowExecutionId) {
    NewRelicMetricAnalysisRecord analysisRecord;

    Query<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecordQuery =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .field("stateExecutionId")
            .equal(stateExecutionId)
            .field("workflowExecutionId")
            .equal(workflowExecutionId);
    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord =
        wingsPersistence.executeGetOneQuery(timeSeriesMLAnalysisRecordQuery);
    if (timeSeriesMLAnalysisRecord != null) {
      List<NewRelicMetricAnalysis> metricAnalysisList = new ArrayList<>();
      for (TimeSeriesMLTxnSummary txnSummary : timeSeriesMLAnalysisRecord.getTransactions().values()) {
        List<NewRelicMetricAnalysisValue> metricsList = new ArrayList<>();
        RiskLevel globalRisk = RiskLevel.NA;
        for (TimeSeriesMLMetricSummary mlMetricSummary : txnSummary.getMetrics().values()) {
          RiskLevel riskLevel;
          switch (mlMetricSummary.getMax_risk()) {
            case -1:
              riskLevel = RiskLevel.NA;
              break;
            case 0:
              riskLevel = RiskLevel.LOW;
              break;
            case 1:
              riskLevel = RiskLevel.MEDIUM;
              break;
            case 2:
              riskLevel = RiskLevel.HIGH;
              break;
            default:
              throw new RuntimeException("Unknown risk level " + mlMetricSummary.getMax_risk());
          }
          if (riskLevel.compareTo(globalRisk) < 0) {
            globalRisk = riskLevel;
          }
          metricsList.add(NewRelicMetricAnalysisValue.builder()
                              .name(mlMetricSummary.getMetric_name())
                              .riskLevel(riskLevel)
                              .controlValue(mlMetricSummary.getControl_avg())
                              .testValue(mlMetricSummary.getTest_avg())
                              .build());
        }
        metricAnalysisList.add(NewRelicMetricAnalysis.builder()
                                   .metricName(txnSummary.getTxn_name())
                                   .metricValues(metricsList)
                                   .riskLevel(globalRisk)
                                   .build());
      }
      analysisRecord = NewRelicMetricAnalysisRecord.builder()
                           .applicationId(timeSeriesMLAnalysisRecord.getApplicationId())
                           .analysisMinute(timeSeriesMLAnalysisRecord.getAnalysisMinute())
                           .metricAnalyses(metricAnalysisList)
                           .stateExecutionId(timeSeriesMLAnalysisRecord.getStateExecutionId())
                           .workflowExecutionId(timeSeriesMLAnalysisRecord.getWorkflowExecutionId())
                           .build();
    } else {
      Query<NewRelicMetricAnalysisRecord> metricAnalysisRecordQuery =
          wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
              .field("stateExecutionId")
              .equal(stateExecutionId)
              .field("workflowExecutionId")
              .equal(workflowExecutionId)
              .field("stateType")
              .equal(stateType);

      analysisRecord = wingsPersistence.executeGetOneQuery(metricAnalysisRecordQuery);
      if (analysisRecord == null) {
        return null;
      }
    }

    if (analysisRecord.getMetricAnalyses() != null) {
      int highRisk = 0;
      int mediumRisk = 0;
      for (NewRelicMetricAnalysis metricAnalysis : analysisRecord.getMetricAnalyses()) {
        switch (metricAnalysis.getRiskLevel()) {
          case HIGH:
            highRisk++;
            break;
          case MEDIUM:
            mediumRisk++;
            break;
        }
      }

      if (highRisk == 0 && mediumRisk == 0) {
        analysisRecord.setMessage("No problems found");
      } else {
        String message = "";
        if (highRisk > 0) {
          message = highRisk + " high risk " + (highRisk > 1 ? "transactions" : "transaction") + " found. ";
        }

        if (mediumRisk > 0) {
          message += mediumRisk + " medium risk " + (mediumRisk > 1 ? "transactions" : "transaction") + " found.";
        }

        analysisRecord.setMessage(message);
      }

      if (highRisk > 0) {
        analysisRecord.setRiskLevel(RiskLevel.HIGH);
      } else if (mediumRisk > 0) {
        analysisRecord.setRiskLevel(RiskLevel.MEDIUM);
      }

      Collections.sort(analysisRecord.getMetricAnalyses());
    }
    return analysisRecord;
  }

  public NewRelicMetricAnalysisRecord _getMetricsAnalysis(
      StateType stateType, String stateExecutionId, String workflowExecutionId) {
    Query<NewRelicMetricAnalysisRecord> splunkLogMLAnalysisRecords =
        wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
            .field("stateType")
            .equal(stateType)
            .field("stateExecutionId")
            .equal(stateExecutionId)
            .field("workflowExecutionId")
            .equal(workflowExecutionId);
    NewRelicMetricAnalysisRecord analysisRecord = wingsPersistence.executeGetOneQuery(splunkLogMLAnalysisRecords);
    if (analysisRecord == null) {
      return null;
    }

    if (analysisRecord.getMetricAnalyses() == null) {
      return NewRelicMetricAnalysisRecord.builder()
          .message(
              "Could not get metric data from new relic. Please make sure that the new relic account is a paid account and metrics can be pulled using rest API")
          .build();
    }

    int highRisk = 0;
    int mediumRisk = 0;
    for (NewRelicMetricAnalysis metricAnalysis : analysisRecord.getMetricAnalyses()) {
      switch (metricAnalysis.getRiskLevel()) {
        case HIGH:
          highRisk++;
          break;
        case MEDIUM:
          mediumRisk++;
          break;
      }
    }

    if (highRisk == 0 && mediumRisk == 0) {
      analysisRecord.setMessage("No problems found");
    } else {
      String message = "";
      if (highRisk > 0) {
        message = highRisk + " high risk " + (highRisk > 1 ? "transactions" : "transaction") + " found. ";
      }

      if (mediumRisk > 0) {
        message += mediumRisk + " medium risk " + (mediumRisk > 1 ? "transactions" : "transaction") + " found.";
      }

      analysisRecord.setMessage(message);
    }

    Collections.sort(analysisRecord.getMetricAnalyses());
    return analysisRecord;
  }

  @Override
  public boolean isStateValid(String appdId, String stateExecutionID) {
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(appdId, stateExecutionID);
    return (stateExecutionInstance == null || stateExecutionInstance.getStatus().isFinalStatus()) ? false : true;
  }

  @Override
  public int getCollectionMinuteToProcess(
      StateType stateType, String stateExecutionId, String workflowExecutionId, String serviceId) {
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .field("stateType")
                                                .equal(stateType)
                                                .field("workflowExecutionId")
                                                .equal(workflowExecutionId)
                                                .field("stateExecutionId")
                                                .equal(stateExecutionId)
                                                .field("serviceId")
                                                .equal(serviceId)
                                                .field("level")
                                                .equal(ClusterLevel.HF)
                                                .order("-dataCollectionMinute")
                                                .limit(1);

    if (query.asList().size() == 0) {
      logger.info(
          "No metric record with heartbeat level {} found for stateExecutionId: {}, workflowExecutionId: {}, serviceId: {}. Will be running analysis for minute 0",
          ClusterLevel.HF, stateExecutionId, workflowExecutionId, serviceId);
      return 0;
    }

    return query.asList().get(0).getDataCollectionMinute() + 1;
  }

  @Override
  public void bumpCollectionMinuteToProcess(
      StateType stateType, String stateExecutionId, String workflowExecutionId, String serviceId, int analysisMinute) {
    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .field("stateType")
                                                .equal(stateType)
                                                .field("workflowExecutionId")
                                                .equal(workflowExecutionId)
                                                .field("stateExecutionId")
                                                .equal(stateExecutionId)
                                                .field("serviceId")
                                                .equal(serviceId)
                                                .field("level")
                                                .equal(ClusterLevel.H0)
                                                .field("dataCollectionMinute")
                                                .lessThanOrEq(analysisMinute);

    wingsPersistence.update(
        query, wingsPersistence.createUpdateOperations(NewRelicMetricDataRecord.class).set("level", ClusterLevel.HF));
  }
}
