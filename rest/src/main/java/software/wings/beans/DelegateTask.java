package software.wings.beans;

import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.common.Constants.DEFAULT_SYNC_CALL_TIMEOUT;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.AlsoLoad;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import software.wings.beans.DelegateTask.Converter;
import software.wings.delegatetasks.DelegateRunnableTask;
import software.wings.utils.KryoUtils;

import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 12/5/16.
 */
@Entity(value = "delegateTasks", noClassnameStored = true)
@Converters(Converter.class)
public class DelegateTask extends Base {
  @NotNull private TaskType taskType;
  private Object[] parameters;
  private String tag;
  @NotEmpty private String accountId;
  private String waitId;
  @AlsoLoad("topicName") private String queueName;
  private Status status = Status.QUEUED;
  private String delegateId;
  private long timeout = DEFAULT_ASYNC_CALL_TIMEOUT;
  private boolean async = true;
  private String envId;

  // TODO(brett): Store envId, serviceID, etc, for delegate task filtering

  @Transient private transient DelegateRunnableTask delegateRunnableTask;

  /**
   * Is timed out boolean.
   *
   * @return the boolean
   */
  public boolean isTimedOut() {
    return getLastUpdatedAt() + timeout <= System.currentTimeMillis();
  }

  /**
   * Getter for property 'taskType'.
   *
   * @return Value for property 'taskType'.
   */
  public TaskType getTaskType() {
    return taskType;
  }

  /**
   * Setter for property 'taskType'.
   *
   * @param taskType Value to set for property 'taskType'.
   */
  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  /**
   * Getter for property 'parameters'.
   *
   * @return Value for property 'parameters'.
   */
  public Object[] getParameters() {
    return parameters;
  }

  /**
   * Setter for property 'parameters'.
   *
   * @param parameters Value to set for property 'parameters'.
   */
  public void setParameters(Object[] parameters) {
    this.parameters = parameters;
  }

  /**
   * Getter for property 'tag'.
   *
   * @return Value for property 'tag'.
   */
  public String getTag() {
    return tag;
  }

  /**
   * Setter for property 'tag'.
   *
   * @param tag Value to set for property 'tag'.
   */
  public void setTag(String tag) {
    this.tag = tag;
  }

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * Getter for property 'waitId'.
   *
   * @return Value for property 'waitId'.
   */
  public String getWaitId() {
    return waitId;
  }

  /**
   * Setter for property 'waitId'.
   *
   * @param waitId Value to set for property 'waitId'.
   */
  public void setWaitId(String waitId) {
    this.waitId = waitId;
  }

  /**
   * Getter for property 'queueName'.
   *
   * @return Value for property 'queueName'.
   */
  public String getQueueName() {
    return queueName;
  }

  /**
   * Setter for property 'queueName'.
   *
   * @param queueName Value to set for property 'queueName'.
   */
  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  /**
   * Getter for property 'status'.
   *
   * @return Value for property 'status'.
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Setter for property 'status'.
   *
   * @param status Value to set for property 'status'.
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * Getter for property 'delegateId'.
   *
   * @return Value for property 'delegateId'.
   */
  public String getDelegateId() {
    return delegateId;
  }

  /**
   * Setter for property 'delegateId'.
   *
   * @param delegateId Value to set for property 'delegateId'.
   */
  public void setDelegateId(String delegateId) {
    this.delegateId = delegateId;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(taskType, parameters, tag, accountId, waitId, queueName, status, delegateId, envId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final DelegateTask other = (DelegateTask) obj;
    return Objects.equals(this.taskType, other.taskType) && Objects.deepEquals(this.parameters, other.parameters)
        && Objects.equals(this.tag, other.tag) && Objects.equals(this.accountId, other.accountId)
        && Objects.equals(this.waitId, other.waitId) && Objects.equals(this.queueName, other.queueName)
        && Objects.equals(this.status, other.status) && Objects.equals(this.delegateId, other.delegateId)
        && Objects.equals(this.envId, other.envId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("taskType", taskType)
        .add("tag", tag)
        .add("accountId", accountId)
        .add("waitId", waitId)
        .add("queueName", queueName)
        .add("status", status)
        .add("delegateId", delegateId)
        .add("envId", envId)
        .toString();
  }

  /**
   * Gets delegate runnable task.
   *
   * @return the delegate runnable task
   */
  public DelegateRunnableTask getDelegateRunnableTask() {
    return delegateRunnableTask;
  }

  /**
   * Sets delegate runnable task.
   *
   * @param delegateRunnableTask the delegate runnable task
   */
  public void setDelegateRunnableTask(DelegateRunnableTask delegateRunnableTask) {
    this.delegateRunnableTask = delegateRunnableTask;
  }

  /**
   * Gets timeout.
   *
   * @return the timeout
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * Sets timeout.
   *
   * @param timeout the timeout
   */
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  /**
   * Is async boolean.
   *
   * @return the boolean
   */
  public boolean isAsync() {
    return async;
  }

  /**
   * Sets async.
   *
   * @param async the async
   */
  public void setAsync(boolean async) {
    this.async = async;
  }

  /**
   * The type Context.
   */
  public static class SyncTaskContext {
    private String accountId;
    private String appId;
    private long timeOut = DEFAULT_SYNC_CALL_TIMEOUT;

    /**
     * Getter for property 'accountId'.
     *
     * @return Value for property 'accountId'.
     */
    public String getAccountId() {
      return accountId;
    }

    /**
     * Setter for property 'accountId'.
     *
     * @param accountId Value to set for property 'accountId'.
     */
    public void setAccountId(String accountId) {
      this.accountId = accountId;
    }

    /**
     * Getter for property 'appId'.
     *
     * @return Value for property 'appId'.
     */
    public String getAppId() {
      return appId;
    }

    /**
     * Setter for property 'appId'.
     *
     * @param appId Value to set for property 'appId'.
     */
    public void setAppId(String appId) {
      this.appId = appId;
    }

    /**
     * Gets time out.
     *
     * @return the time out
     */
    public long getTimeOut() {
      return timeOut;
    }

    /**
     * Sets time out.
     *
     * @param timeOut the time out
     */
    public void setTimeOut(long timeOut) {
      this.timeOut = timeOut;
    }

    /**
     * The type Builder.
     */
    public static final class Builder {
      private String accountId;
      private String appId;

      private Builder() {}

      /**
       * A context builder.
       *
       * @return the builder
       */
      public static Builder aContext() {
        return new Builder();
      }

      /**
       * With account id builder.
       *
       * @param accountId the account id
       * @return the builder
       */
      public Builder withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
      }

      /**
       * With app id builder.
       *
       * @param appId the app id
       * @return the builder
       */
      public Builder withAppId(String appId) {
        this.appId = appId;
        return this;
      }

      /**
       * But builder.
       *
       * @return the builder
       */
      public Builder but() {
        return aContext().withAccountId(accountId).withAppId(appId);
      }

      /**
       * Build context.
       *
       * @return the context
       */
      public SyncTaskContext build() {
        SyncTaskContext syncTaskContext = new SyncTaskContext();
        syncTaskContext.setAccountId(accountId);
        syncTaskContext.setAppId(appId);
        return syncTaskContext;
      }
    }
  }

  /**
   * The type Converter.
   */
  public static class Converter extends TypeConverter {
    /**
     * Instantiates a new Converter.
     */
    public Converter() {
      super(Object[].class);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
      return KryoUtils.asString(value);
    }

    @Override
    public Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
      return KryoUtils.asObject((String) fromDBObject);
    }
  }

  /**
   * The enum Status.
   */
  public enum Status {
    /**
     * Queued status.
     */
    QUEUED, /**
             * Started status.
             */
    STARTED, /**
              * Finished status.
              */
    FINISHED, /**
               * Error status.
               */
    ERROR, /**
            * Aborted status.
            */
    ABORTED
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private TaskType taskType;
    private Object[] parameters;
    private String tag;
    private String accountId;
    private String waitId;
    private String queueName;
    private Status status = Status.QUEUED;
    private String delegateId;
    private String envId;
    private long timeout = DEFAULT_ASYNC_CALL_TIMEOUT;
    private String uuid;
    private boolean async = true;
    private String appId;
    private transient DelegateRunnableTask delegateRunnableTask;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A delegate task builder.
     *
     * @return the builder
     */
    public static Builder aDelegateTask() {
      return new Builder();
    }

    /**
     * With task type builder.
     *
     * @param taskType the task type
     * @return the builder
     */
    public Builder withTaskType(TaskType taskType) {
      this.taskType = taskType;
      return this;
    }

    /**
     * With parameters builder.
     *
     * @param parameters the parameters
     * @return the builder
     */
    public Builder withParameters(Object[] parameters) {
      this.parameters = parameters;
      return this;
    }

    /**
     * With tag builder.
     *
     * @param tag the tag
     * @return the builder
     */
    public Builder withTag(String tag) {
      this.tag = tag;
      return this;
    }

    /**
     * With account id builder.
     *
     * @param accountId the account id
     * @return the builder
     */
    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    /**
     * With wait id builder.
     *
     * @param waitId the wait id
     * @return the builder
     */
    public Builder withWaitId(String waitId) {
      this.waitId = waitId;
      return this;
    }

    /**
     * With queue name builder.
     *
     * @param queueName the queue name
     * @return the builder
     */
    public Builder withQueueName(String queueName) {
      this.queueName = queueName;
      return this;
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(Status status) {
      this.status = status;
      return this;
    }

    /**
     * With delegate id builder.
     *
     * @param delegateId the delegate id
     * @return the builder
     */
    public Builder withDelegateId(String delegateId) {
      this.delegateId = delegateId;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With timeout builder.
     *
     * @param timeout the timeout
     * @return the builder
     */
    public Builder withTimeout(long timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With async builder.
     *
     * @param async the async
     * @return the builder
     */
    public Builder withAsync(boolean async) {
      this.async = async;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With delegate runnable task builder.
     *
     * @param delegateRunnableTask the delegate runnable task
     * @return the builder
     */
    public Builder withDelegateRunnableTask(DelegateRunnableTask delegateRunnableTask) {
      this.delegateRunnableTask = delegateRunnableTask;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aDelegateTask()
          .withTaskType(taskType)
          .withParameters(parameters)
          .withTag(tag)
          .withAccountId(accountId)
          .withWaitId(waitId)
          .withQueueName(queueName)
          .withStatus(status)
          .withDelegateId(delegateId)
          .withEnvId(envId)
          .withTimeout(timeout)
          .withUuid(uuid)
          .withAsync(async)
          .withAppId(appId)
          .withDelegateRunnableTask(delegateRunnableTask)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build delegate task.
     *
     * @return the delegate task
     */
    public DelegateTask build() {
      DelegateTask delegateTask = new DelegateTask();
      delegateTask.setTaskType(taskType);
      delegateTask.setParameters(parameters);
      delegateTask.setTag(tag);
      delegateTask.setAccountId(accountId);
      delegateTask.setWaitId(waitId);
      delegateTask.setQueueName(queueName);
      delegateTask.setStatus(status);
      delegateTask.setDelegateId(delegateId);
      delegateTask.setEnvId(envId);
      delegateTask.setTimeout(timeout);
      delegateTask.setUuid(uuid);
      delegateTask.setAsync(async);
      delegateTask.setAppId(appId);
      delegateTask.setDelegateRunnableTask(delegateRunnableTask);
      delegateTask.setCreatedBy(createdBy);
      delegateTask.setCreatedAt(createdAt);
      delegateTask.setLastUpdatedBy(lastUpdatedBy);
      delegateTask.setLastUpdatedAt(lastUpdatedAt);
      return delegateTask;
    }
  }
}
