package io.harness.metrics.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.NgSetupFields.NG;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.metrics.AutoMetricContext;
import io.harness.metrics.beans.DelegateMetricContext;
import io.harness.metrics.beans.DelegateTaskMetricContext;
import io.harness.metrics.beans.DelegateTaskResponseMetricContext;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class DelegateTaskMetricContextBuilder {
  private static Map<Class<?>, DelegateTaskMetricContextBuilder.ObjectContextBuilder<?>> OBJECT_CONTEXT_BUILDER_MAP =
      new HashMap<>();
  static {
    addToObjContextMap(DelegateTask.class,
        delegateTask
        -> new DelegateTaskMetricContext(delegateTask.getAccountId(), delegateTask.getDelegateId(),
            delegateTask.getStatus(), delegateTask.getVersion(), isNg(delegateTask),
            delegateTask.getData().getTaskType(), delegateTask.getData().isAsync()));

    addToObjContextMap(DelegateTaskResponse.class,
        response
        -> new DelegateTaskResponseMetricContext(
            response.getAccountId(), response.getResponseCode(), response.getResponse().getClass()));
    addToObjContextMap(Delegate.class,
        delegate
        -> new DelegateMetricContext(delegate.getAccountId(), delegate.getUuid(), delegate.getLastHeartBeat(),
            delegate.isNg(), delegate.getStatus().name(), delegate.getVersion()));
  }

  private static <T> void addToObjContextMap(
      Class<T> clazz, DelegateTaskMetricContextBuilder.ObjectContextBuilder<T> objectContextBuilder) {
    OBJECT_CONTEXT_BUILDER_MAP.put(clazz, objectContextBuilder);
  }

  public <T> AutoMetricContext getContext(T obj, Class<T> clazz) {
    Preconditions.checkState(OBJECT_CONTEXT_BUILDER_MAP.containsKey(clazz),
        "Object context builder is not defined for class: %s", obj.getClass());
    DelegateTaskMetricContextBuilder.ObjectContextBuilder<T> objectContextBuilder =
        (DelegateTaskMetricContextBuilder.ObjectContextBuilder<T>) OBJECT_CONTEXT_BUILDER_MAP.get(clazz);
    return objectContextBuilder.create(obj);
  }

  private interface ObjectContextBuilder<T> {
    AutoMetricContext create(T obj);
  }

  private static boolean isNg(DelegateTask delegateTask) {
    return !isEmpty(delegateTask.getSetupAbstractions())
        && Boolean.parseBoolean(delegateTask.getSetupAbstractions().get(NG));
  }
}
