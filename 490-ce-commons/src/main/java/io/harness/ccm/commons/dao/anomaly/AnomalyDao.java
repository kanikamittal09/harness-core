/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.dao.anomaly;

import static io.harness.timescaledb.Tables.ANOMALIES;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.jooq.impl.DSL.sum;

import io.harness.annotations.retry.RetryOnException;
import io.harness.ccm.commons.entities.anomaly.AnomalyFeedbackDTO;
import io.harness.ccm.commons.entities.anomaly.AnomalySummary;
import io.harness.timescaledb.tables.pojos.Anomalies;
import io.harness.timescaledb.tables.records.AnomaliesRecord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.istack.internal.Nullable;
import java.math.BigDecimal;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record3;
import org.jooq.SelectConditionStep;
import org.jooq.SelectFinalStep;
import org.jooq.impl.DSL;

@Slf4j
@Singleton
public class AnomalyDao {
  @Inject private DSLContext dslContext;

  private static final int RETRY_COUNT = 3;
  private static final int SLEEP_DURATION = 100;

  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public List<Anomalies> fetchAnomalies(@NonNull String accountId, @Nullable Condition condition,
      @NonNull List<OrderField<?>> orderFields, @NonNull Integer offset, @NonNull Integer limit) {
    SelectFinalStep<AnomaliesRecord> finalStep =
        dslContext.selectFrom(ANOMALIES)
            .where(ANOMALIES.ACCOUNTID.eq(accountId).and(firstNonNull(condition, DSL.noCondition())))
            .orderBy(orderFields)
            .offset(offset)
            .limit(limit);
    log.info("Anomaly Query: {}", finalStep.getQuery().toString());
    return finalStep.fetchInto(Anomalies.class);
  }

  @Nullable
  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public List<AnomalySummary> fetchAnomaliesTotalCost(@NonNull String accountId, @Nullable Condition condition) {
    SelectConditionStep<Record3<Integer, BigDecimal, BigDecimal>> finalStep =
        dslContext
            .select(DSL.count().as("count"), sum(ANOMALIES.ACTUALCOST).as("actualCost"),
                sum(ANOMALIES.EXPECTEDCOST).as("expectedCost"))
            .from(ANOMALIES)
            .where(ANOMALIES.ACCOUNTID.eq(accountId).and(firstNonNull(condition, DSL.noCondition())));
    log.info("Anomaly Query: {}", finalStep.getQuery().toString());
    return finalStep.fetchInto(AnomalySummary.class);
  }

  @Nullable
  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void updateAnomalyFeedback(@NonNull String accountId, String anomalyId, AnomalyFeedbackDTO feedback) {
    dslContext.update(ANOMALIES)
        .set(ANOMALIES.FEEDBACK, feedback.getFeedback().toString())
        .where(ANOMALIES.ACCOUNTID.eq(accountId).and(ANOMALIES.ID.eq(anomalyId)))
        .execute();
  }
}
