package io.harness.delegate.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateTimeBasedUuid;
import static io.harness.delegate.app.DelegateApplication.getProcessId;
import static io.harness.delegate.configuration.InstallUtils.installChartMuseum;
import static io.harness.delegate.configuration.InstallUtils.installGoTemplateTool;
import static io.harness.delegate.configuration.InstallUtils.installHarnessPywinrm;
import static io.harness.delegate.configuration.InstallUtils.installHelm;
import static io.harness.delegate.configuration.InstallUtils.installKubectl;
import static io.harness.delegate.configuration.InstallUtils.installKustomize;
import static io.harness.delegate.configuration.InstallUtils.installOc;
import static io.harness.delegate.configuration.InstallUtils.installScm;
import static io.harness.delegate.configuration.InstallUtils.installTerraformConfigInspect;
import static io.harness.delegate.configuration.InstallUtils.validateCfCliExists;
import static io.harness.delegate.message.ManagerMessageConstants.JRE_VERSION;
import static io.harness.delegate.message.ManagerMessageConstants.MIGRATE;
import static io.harness.delegate.message.ManagerMessageConstants.SELF_DESTRUCT;
import static io.harness.delegate.message.ManagerMessageConstants.UPDATE_PERPETUAL_TASK;
import static io.harness.delegate.message.ManagerMessageConstants.USE_CDN;
import static io.harness.delegate.message.ManagerMessageConstants.USE_STORAGE_PROXY;
import static io.harness.delegate.message.MessageConstants.DELEGATE_DASH;
import static io.harness.delegate.message.MessageConstants.DELEGATE_GO_AHEAD;
import static io.harness.delegate.message.MessageConstants.DELEGATE_HEARTBEAT;
import static io.harness.delegate.message.MessageConstants.DELEGATE_IS_NEW;
import static io.harness.delegate.message.MessageConstants.DELEGATE_JRE_VERSION;
import static io.harness.delegate.message.MessageConstants.DELEGATE_MIGRATE;
import static io.harness.delegate.message.MessageConstants.DELEGATE_RESTART_NEEDED;
import static io.harness.delegate.message.MessageConstants.DELEGATE_RESUME;
import static io.harness.delegate.message.MessageConstants.DELEGATE_SELF_DESTRUCT;
import static io.harness.delegate.message.MessageConstants.DELEGATE_SEND_VERSION_HEADER;
import static io.harness.delegate.message.MessageConstants.DELEGATE_SHUTDOWN_PENDING;
import static io.harness.delegate.message.MessageConstants.DELEGATE_SHUTDOWN_STARTED;
import static io.harness.delegate.message.MessageConstants.DELEGATE_STARTED;
import static io.harness.delegate.message.MessageConstants.DELEGATE_START_GRPC;
import static io.harness.delegate.message.MessageConstants.DELEGATE_STOP_ACQUIRING;
import static io.harness.delegate.message.MessageConstants.DELEGATE_STOP_GRPC;
import static io.harness.delegate.message.MessageConstants.DELEGATE_SWITCH_STORAGE;
import static io.harness.delegate.message.MessageConstants.DELEGATE_UPGRADE_NEEDED;
import static io.harness.delegate.message.MessageConstants.DELEGATE_UPGRADE_PENDING;
import static io.harness.delegate.message.MessageConstants.DELEGATE_UPGRADE_STARTED;
import static io.harness.delegate.message.MessageConstants.DELEGATE_VERSION;
import static io.harness.delegate.message.MessageConstants.MIGRATE_TO_JRE_VERSION;
import static io.harness.delegate.message.MessageConstants.UPGRADING_DELEGATE;
import static io.harness.delegate.message.MessageConstants.WATCHER_DATA;
import static io.harness.delegate.message.MessageConstants.WATCHER_HEARTBEAT;
import static io.harness.delegate.message.MessageConstants.WATCHER_PROCESS;
import static io.harness.delegate.message.MessageConstants.WATCHER_VERSION;
import static io.harness.delegate.message.MessengerType.DELEGATE;
import static io.harness.delegate.message.MessengerType.WATCHER;
import static io.harness.eraro.ErrorCode.EXPIRED_TOKEN;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.eraro.ErrorCode.REVOKED_TOKEN;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.filesystem.FileIo.acquireLock;
import static io.harness.filesystem.FileIo.isLocked;
import static io.harness.filesystem.FileIo.releaseLock;
import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;
import static io.harness.logging.Misc.getDurationString;
import static io.harness.network.Localhost.getLocalHostAddress;
import static io.harness.network.Localhost.getLocalHostName;
import static io.harness.network.SafeHttpCall.execute;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.apache.commons.io.filefilter.FileFilterUtils.falseFileFilter;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DelegateHeartbeatResponse;
import io.harness.beans.DelegateTaskEventsResponse;
import io.harness.configuration.DeployMode;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateConnectionHeartbeat;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.beans.DelegateParams;
import io.harness.delegate.beans.DelegateParams.DelegateParamsBuilder;
import io.harness.delegate.beans.DelegateProfileParams;
import io.harness.delegate.beans.DelegateRegisterResponse;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateTaskAbortEvent;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.SecretDetail;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.delegate.configuration.InstallUtils;
import io.harness.delegate.expression.DelegateExpressionEvaluator;
import io.harness.delegate.logging.DelegateStackdriverLogAppender;
import io.harness.delegate.message.Message;
import io.harness.delegate.message.MessageService;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.Cd1ApplicationAccess;
import io.harness.delegate.task.DelegateRunnableTask;
import io.harness.delegate.task.TaskLogContext;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.validation.DelegateConnectionResultDetail;
import io.harness.exception.UnexpectedException;
import io.harness.expression.ExpressionReflectionUtils;
import io.harness.filesystem.FileIo;
import io.harness.grpc.DelegateServiceGrpcAgentClient;
import io.harness.grpc.util.RestartableServiceManager;
import io.harness.logging.AutoLogContext;
import io.harness.logstreaming.LogStreamingClient;
import io.harness.logstreaming.LogStreamingHelper;
import io.harness.logstreaming.LogStreamingSanitizer;
import io.harness.logstreaming.LogStreamingTaskClient;
import io.harness.logstreaming.LogStreamingTaskClient.LogStreamingTaskClientBuilder;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.managerclient.DelegateAgentManagerClientFactory;
import io.harness.network.FibonacciBackOff;
import io.harness.network.Http;
import io.harness.perpetualtask.PerpetualTaskWorker;
import io.harness.rest.RestResponse;
import io.harness.security.TokenGenerator;
import io.harness.security.encryption.DelegateDecryptionService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.serializer.JsonUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.taskprogress.TaskProgressClient;
import io.harness.threading.Schedulable;
import io.harness.utils.ProcessControl;
import io.harness.version.VersionInfoManager;

import software.wings.beans.DelegateTaskFactory;
import software.wings.beans.TaskType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.delegation.CommandParameters;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.beans.shellscript.provisioner.ShellScriptProvisionParameters;
import software.wings.delegatetasks.ActivityBasedLogSanitizer;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.GenericLogSanitizer;
import software.wings.delegatetasks.LogSanitizer;
import software.wings.delegatetasks.delegatecapability.CapabilityCheckController;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.delegatetasks.validation.DelegateValidateTask;
import software.wings.service.intfc.security.EncryptionService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.ning.http.client.AsyncHttpClient;
import com.sun.management.OperatingSystemMXBean;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.net.ssl.SSLException;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody.Part;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Precision;
import org.apache.http.client.utils.URIBuilder;
import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.Encoder;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Options;
import org.atmosphere.wasync.Request.METHOD;
import org.atmosphere.wasync.Request.TRANSPORT;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.Socket.STATUS;
import org.atmosphere.wasync.transport.TransportNotSupported;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import retrofit2.Call;
import retrofit2.Response;

@Singleton
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
@BreakDependencyOn("software.wings.delegatetasks.validation.DelegateConnectionResult")
@BreakDependencyOn("io.harness.delegate.beans.Delegate")
@OwnedBy(HarnessTeam.DEL)
public class DelegateAgentServiceImpl implements DelegateAgentService {
  private static final int POLL_INTERVAL_SECONDS = 3;
  private static final long UPGRADE_TIMEOUT = TimeUnit.HOURS.toMillis(2);
  private static final long HEARTBEAT_TIMEOUT = TimeUnit.MINUTES.toMillis(15);
  private static final long FROZEN_TIMEOUT = TimeUnit.HOURS.toMillis(2);
  private static final long WATCHER_HEARTBEAT_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
  private static final long WATCHER_VERSION_MATCH_TIMEOUT = TimeUnit.MINUTES.toMillis(2);
  private static final long DELEGATE_JRE_VERSION_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
  private static final String DELEGATE_SEQUENCE_CONFIG_FILE = "./delegate_sequence_config";
  private static final int KEEP_ALIVE_INTERVAL = 23000;
  private static final int CLIENT_TOOL_RETRIES = 10;
  private static final String TOKEN = "[TOKEN]";
  private static final String SEQ = "[SEQ]";

  private static final String HOST_NAME = getLocalHostName();
  private static final String DELEGATE_TYPE = System.getenv().get("DELEGATE_TYPE");
  private static final String DELEGATE_GROUP_NAME = System.getenv().get("DELEGATE_GROUP_NAME");
  private final String delegateGroupId = System.getenv().get("DELEGATE_GROUP_ID");

  private static final String START_SH = "start.sh";
  private static final String DUPLICATE_DELEGATE_ERROR_MESSAGE =
      "Duplicate delegate with same delegateId:%s and connectionId:%s exists";

  private final String delegateSessionIdentifier = System.getenv().get("DELEGATE_SESSION_IDENTIFIER");
  private final String delegateOrgIdentifier = System.getenv().get("DELEGATE_ORG_IDENTIFIER");
  private final String delegateProjectIdentifier = System.getenv().get("DELEGATE_PROJECT_IDENTIFIER");
  private final String delegateSize = System.getenv().get("DELEGATE_SIZE");
  private final String delegateDescription = System.getenv().get("DELEGATE_DESCRIPTION");
  private final int delegateTaskLimit = isNotBlank(System.getenv().get("DELEGATE_TASK_LIMIT"))
      ? Integer.parseInt(System.getenv().get("DELEGATE_TASK_LIMIT"))
      : 0;

  public static final String JAVA_VERSION = "java.version";

  private static volatile String delegateId;

  @Inject private DelegateConfiguration delegateConfiguration;
  @Inject private RestartableServiceManager restartableServiceManager;

  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;

  @Inject @Named("heartbeatExecutor") private ScheduledExecutorService heartbeatExecutor;
  @Inject @Named("localHeartbeatExecutor") private ScheduledExecutorService localHeartbeatExecutor;
  @Inject @Named("upgradeExecutor") private ScheduledExecutorService upgradeExecutor;
  @Inject @Named("inputExecutor") private ScheduledExecutorService inputExecutor;
  @Inject @Named("rescheduleExecutor") private ScheduledExecutorService rescheduleExecutor;
  @Inject @Named("installCheckExecutor") private ScheduledExecutorService profileExecutor;
  @Inject @Named("systemExecutor") private ExecutorService systemExecutor;
  @Inject @Named("taskPollExecutor") private ExecutorService taskPollExecutor;
  @Inject @Named("asyncExecutor") private ExecutorService asyncExecutor;
  @Inject @Named("asyncTaskDispatchExecutor") private ExecutorService asyncTaskDispatchExecutor;
  @Inject @Named("artifactExecutor") private ExecutorService artifactExecutor;
  @Inject @Named("timeoutExecutor") private ExecutorService timeoutEnforcement;
  @Inject @Named("grpcServiceExecutor") private ExecutorService grpcServiceExecutor;
  @Inject private ExecutorService syncExecutor;

  @Inject private SignalService signalService;
  @Inject private MessageService messageService;
  @Inject private Injector injector;
  @Inject private TokenGenerator tokenGenerator;
  @Inject private AsyncHttpClient asyncHttpClient;
  @Inject private Clock clock;
  @Inject private TimeLimiter timeLimiter;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private DelegateDecryptionService delegateDecryptionService;
  @Inject private DelegateLogService delegateLogService;
  @Inject private EncryptionService encryptionService;
  @Inject private ExecutionConfigOverrideFromFileOnDelegate delegateLocalConfigService;
  @Inject(optional = true) @Nullable private PerpetualTaskWorker perpetualTaskWorker;
  @Inject(optional = true) @Nullable private LogStreamingClient logStreamingClient;
  @Inject DelegateTaskFactory delegateTaskFactory;
  @Inject(optional = true) @Nullable private DelegateServiceGrpcAgentClient delegateServiceGrpcAgentClient;
  @Inject private KryoSerializer kryoSerializer;

  private final AtomicBoolean waiter = new AtomicBoolean(true);

  private final Set<String> currentlyAcquiringTasks = ConcurrentHashMap.newKeySet();
  private final Map<String, DelegateTaskPackage> currentlyValidatingTasks = new ConcurrentHashMap<>();
  private final Map<String, DelegateTaskPackage> currentlyExecutingTasks = new ConcurrentHashMap<>();
  private final Map<String, Future<?>> currentlyValidatingFutures = new ConcurrentHashMap<>();
  private final Map<String, Future<?>> currentlyExecutingFutures = new ConcurrentHashMap<>();

  private final AtomicInteger maxValidatingTasksCount = new AtomicInteger();
  private final AtomicInteger maxExecutingTasksCount = new AtomicInteger();
  private final AtomicInteger maxValidatingFuturesCount = new AtomicInteger();
  private final AtomicInteger maxExecutingFuturesCount = new AtomicInteger();

  private final AtomicLong lastHeartbeatSentAt = new AtomicLong(System.currentTimeMillis());
  private final AtomicLong frozenAt = new AtomicLong(-1);
  private final AtomicLong lastHeartbeatReceivedAt = new AtomicLong(System.currentTimeMillis());
  private final AtomicBoolean upgradePending = new AtomicBoolean(false);
  private final AtomicBoolean upgradeNeeded = new AtomicBoolean(false);
  private final AtomicBoolean restartNeeded = new AtomicBoolean(false);
  private final AtomicBoolean acquireTasks = new AtomicBoolean(true);
  private final AtomicBoolean frozen = new AtomicBoolean(false);
  private final AtomicBoolean executingProfile = new AtomicBoolean(false);
  private final AtomicBoolean selfDestruct = new AtomicBoolean(false);
  private final AtomicBoolean multiVersionWatcherStarted = new AtomicBoolean(false);
  private final AtomicBoolean pollingForTasks = new AtomicBoolean(false);
  private final AtomicBoolean switchStorage = new AtomicBoolean(false);
  private final AtomicBoolean reconnectingSocket = new AtomicBoolean(false);
  private final AtomicBoolean closingSocket = new AtomicBoolean(false);

  private Client client;
  private Socket socket;
  private String upgradeVersion;
  private String migrateTo;
  private long startTime;
  private long upgradeStartedAt;
  private long stoppedAcquiringAt;
  private String accountId;
  private long watcherVersionMatchedAt = System.currentTimeMillis();
  private long delegateJreVersionChangedAt;

  private final String delegateConnectionId = generateTimeBasedUuid();
  private volatile boolean switchStorageMsgSent;
  private DelegateConnectionHeartbeat connectionHeartbeat;
  private String migrateToJreVersion = System.getProperty(JAVA_VERSION);
  private boolean sendJreInformationToWatcher;

  private final boolean multiVersion = DeployMode.KUBERNETES.name().equals(System.getenv().get(DeployMode.DEPLOY_MODE))
      || TRUE.toString().equals(System.getenv().get("MULTI_VERSION"));

  public static Optional<String> getDelegateId() {
    return Optional.ofNullable(delegateId);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void run(boolean watched) {
    try {
      accountId = delegateConfiguration.getAccountId();
      if (perpetualTaskWorker != null) {
        perpetualTaskWorker.setAccountId(accountId);
      }
      log.info("Delegate will start running on JRE {}", System.getProperty(JAVA_VERSION));
      startTime = clock.millis();
      DelegateStackdriverLogAppender.setTimeLimiter(timeLimiter);
      DelegateStackdriverLogAppender.setManagerClient(delegateAgentManagerClient);

      logProxyConfiguration();

      connectionHeartbeat = DelegateConnectionHeartbeat.builder()
                                .delegateConnectionId(delegateConnectionId)
                                .version(getVersion())
                                .location(Paths.get("").toAbsolutePath().toString())
                                .build();

      if (watched) {
        log.info("[New] Delegate process started. Sending confirmation");
        messageService.writeMessage(DELEGATE_STARTED);
        startInputCheck();
        log.info("[New] Waiting for go ahead from watcher");
        Message message = messageService.waitForMessage(DELEGATE_GO_AHEAD, TimeUnit.MINUTES.toMillis(5));
        log.info(message != null ? "[New] Got go-ahead. Proceeding"
                                 : "[New] Timed out waiting for go-ahead. Proceeding anyway");
        messageService.removeData(DELEGATE_DASH + getProcessId(), DELEGATE_IS_NEW);
        startLocalHeartbeat();
      } else {
        log.info("Delegate process started");
        if (delegateConfiguration.isGrpcServiceEnabled()) {
          restartableServiceManager.start();
          Runtime.getRuntime().addShutdownHook(new Thread(() -> restartableServiceManager.stop()));
        }
      }

      boolean kubectlInstalled = installKubectl(delegateConfiguration);
      boolean goTemplateInstalled = installGoTemplateTool(delegateConfiguration);
      boolean harnessPywinrmInstalled = installHarnessPywinrm(delegateConfiguration);
      boolean helmInstalled = installHelm(delegateConfiguration);
      boolean chartMuseumInstalled = installChartMuseum(delegateConfiguration);
      boolean tfConfigInspectInstalled = installTerraformConfigInspect(delegateConfiguration);
      boolean ocInstalled = installOc(delegateConfiguration);
      boolean kustomizeInstalled = installKustomize(delegateConfiguration);
      boolean scmInstalled = installScm(delegateConfiguration);

      logCfCliConfiguration();

      long start = clock.millis();
      String descriptionFromConfigFile = isBlank(delegateDescription) ? "" : delegateDescription;
      String description = "description here".equals(delegateConfiguration.getDescription())
          ? descriptionFromConfigFile
          : delegateConfiguration.getDescription();

      String delegateName = System.getenv().get("DELEGATE_NAME");
      if (isNotBlank(delegateName)) {
        log.info("Registering delegate with delegate name: {}", delegateName);
      } else {
        delegateName = "";
      }

      String delegateProfile = System.getenv().get("DELEGATE_PROFILE");
      if (isNotBlank(delegateProfile)) {
        log.info("Registering delegate with delegate profile: {}", delegateProfile);
      } else {
        delegateProfile = "";
      }

      boolean isSample = "true".equals(System.getenv().get("SAMPLE_DELEGATE"));

      log.info("DELEGATE_TYPE is: " + DELEGATE_TYPE);
      if (isNotBlank(DELEGATE_TYPE)) {
        log.info(
            "Registering delegate with delegate Type: {}, DelegateGroupName: {}", DELEGATE_TYPE, DELEGATE_GROUP_NAME);
      }

      log.info("Delegate Group Id: {}", delegateGroupId);

      DelegateParamsBuilder builder = DelegateParams.builder()
                                          .ip(getLocalHostAddress())
                                          .accountId(accountId)
                                          .sessionIdentifier(delegateSessionIdentifier)
                                          .orgIdentifier(delegateOrgIdentifier)
                                          .projectIdentifier(delegateProjectIdentifier)
                                          .delegateSize(delegateSize)
                                          .hostName(HOST_NAME)
                                          .delegateName(delegateName)
                                          .delegateGroupName(DELEGATE_GROUP_NAME)
                                          .delegateGroupId(delegateGroupId)
                                          .delegateProfileId(delegateProfile)
                                          .description(description)
                                          .version(getVersion())
                                          .delegateType(DELEGATE_TYPE)
                                          //.proxy(set to true if there is a system proxy)
                                          .pollingModeEnabled(delegateConfiguration.isPollForTasks())
                                          .sampleDelegate(isSample)
                                          .location(Paths.get("").toAbsolutePath().toString())
                                          .ceEnabled(Boolean.parseBoolean(System.getenv("ENABlE_CE")));

      delegateId = registerDelegate(builder);
      log.info("[New] Delegate registered in {} ms", clock.millis() - start);
      DelegateStackdriverLogAppender.setDelegateId(delegateId);

      if (isPollingForTasksEnabled()) {
        log.info("Polling is enabled for Delegate");
        pollingForTasks.set(true);
      } else {
        client = org.atmosphere.wasync.ClientFactory.getDefault().newClient();

        RequestBuilder requestBuilder = prepareRequestBuilder();

        Options clientOptions = client.newOptionsBuilder().runtime(asyncHttpClient, true).reconnect(false).build();
        socket = client.create(clientOptions);
        socket
            .on(Event.MESSAGE,
                new Function<String>() { // Do not change this, wasync doesn't like lambdas
                  @Override
                  public void on(String message) {
                    handleMessageSubmit(message);
                  }
                })
            .on(Event.ERROR,
                new Function<Exception>() { // Do not change this, wasync doesn't like lambdas
                  @Override
                  public void on(Exception e) {
                    handleError(e);
                  }
                })
            .on(Event.OPEN,
                new Function<Object>() { // Do not change this, wasync doesn't like lambdas
                  @Override
                  public void on(Object o) {
                    handleOpen(o);
                  }
                })
            .on(Event.CLOSE, new Function<Object>() { // Do not change this, wasync doesn't like lambdas
              @Override
              public void on(Object o) {
                handleClose(o);
              }
            });

        socket.open(requestBuilder.build());

        startHeartbeat(builder, socket);
        startKeepAlivePacket(builder, socket);
      }

      startTaskPolling();
      startHeartbeatWhenPollingEnabled(builder);
      startKeepAliveRequestWhenPollingEnabled(builder);

      if (!multiVersion) {
        startUpgradeCheck(getVersion());
      }

      log.info("Delegate started");
      log.info("Manager Authority:{}, Manager Target:{}", delegateConfiguration.getManagerAuthority(),
          delegateConfiguration.getManagerTarget());

      startProfileCheck();

      if (!kubectlInstalled || !goTemplateInstalled || !helmInstalled || !chartMuseumInstalled
          || !tfConfigInspectInstalled || !harnessPywinrmInstalled || !scmInstalled) {
        systemExecutor.submit(() -> {
          boolean kubectl = kubectlInstalled;
          boolean goTemplate = goTemplateInstalled;
          boolean helm = helmInstalled;
          boolean chartMuseum = chartMuseumInstalled;
          boolean tfConfigInspect = tfConfigInspectInstalled;
          boolean oc = ocInstalled;
          boolean kustomize = kustomizeInstalled;
          boolean harnessPywinrm = harnessPywinrmInstalled;
          boolean scm = scmInstalled;

          int retries = CLIENT_TOOL_RETRIES;
          while ((!kubectl || !goTemplate || !helm || !chartMuseum || !tfConfigInspect || !harnessPywinrm || !scm)
              && retries > 0) {
            sleep(ofSeconds(15L));
            if (!kubectl) {
              kubectl = installKubectl(delegateConfiguration);
            }
            if (!goTemplate) {
              goTemplate = installGoTemplateTool(delegateConfiguration);
            }
            if (!harnessPywinrm) {
              harnessPywinrm = installHarnessPywinrm(delegateConfiguration);
            }
            if (!helm) {
              helm = installHelm(delegateConfiguration);
            }
            if (!chartMuseum) {
              chartMuseum = installChartMuseum(delegateConfiguration);
            }
            if (!tfConfigInspect) {
              tfConfigInspect = installTerraformConfigInspect(delegateConfiguration);
            }
            if (!tfConfigInspect) {
              tfConfigInspect = installTerraformConfigInspect(delegateConfiguration);
            }
            if (!oc) {
              oc = installOc(delegateConfiguration);
            }
            if (!kustomize) {
              kustomize = installKustomize(delegateConfiguration);
            }
            if (!scm) {
              scm = installScm(delegateConfiguration);
            }

            retries--;
          }

          if (!kubectl) {
            log.error("Failed to install kubectl after {} retries", CLIENT_TOOL_RETRIES);
          }
          if (!goTemplate) {
            log.error("Failed to install go-template after {} retries", CLIENT_TOOL_RETRIES);
          }

          if (!harnessPywinrm) {
            log.error("Failed to install harness-pywinrm after {} retries", CLIENT_TOOL_RETRIES);
          }

          if (!helm) {
            log.error("Failed to install helm after {} retries", CLIENT_TOOL_RETRIES);
          }
          if (!chartMuseum) {
            log.error("Failed to install chartMuseum after {} retries", CLIENT_TOOL_RETRIES);
          }
          if (!tfConfigInspect) {
            log.error("Failed to install tf-config-inspect after {} retries", CLIENT_TOOL_RETRIES);
          }
          if (!kustomize) {
            log.error("Failed to install kustomize after {} retries", CLIENT_TOOL_RETRIES);
          }
          if (!scm) {
            log.error("Failed to install scm after {} retries", CLIENT_TOOL_RETRIES);
          }
        });
      }

      if (delegateLocalConfigService.isLocalConfigPresent()) {
        Map<String, String> localSecrets = delegateLocalConfigService.getLocalDelegateSecrets();
        if (isNotEmpty(localSecrets)) {
          delegateLogService.registerLogSanitizer(new GenericLogSanitizer(new HashSet<>(localSecrets.values())));
        }
      }

      synchronized (waiter) {
        while (waiter.get()) {
          waiter.wait();
        }
      }

      messageService.closeData(DELEGATE_DASH + getProcessId());
      messageService.closeChannel(DELEGATE, getProcessId());

      if (upgradePending.get()) {
        removeDelegateVersionFromCapsule();
        cleanupOldDelegateVersionFromBackup();
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Exception while starting/running delegate", e);
    } catch (RuntimeException | IOException e) {
      log.error("Exception while starting/running delegate", e);
    }
  }

  private RequestBuilder prepareRequestBuilder() {
    try {
      URIBuilder uriBuilder =
          new URIBuilder(delegateConfiguration.getManagerUrl().replace("/api/", "/stream/") + "delegate/" + accountId)
              .addParameter("delegateId", delegateId)
              .addParameter("delegateConnectionId", delegateConnectionId)
              .addParameter("token", tokenGenerator.getToken("https", "localhost", 9090, HOST_NAME))
              .addParameter("sequenceNum", getSequenceNumForEcsDelegate())
              .addParameter("delegateToken", getRandomTokenForEcsDelegate());

      URI uri = uriBuilder.build();

      // Stream the request body
      RequestBuilder requestBuilder =
          client.newRequestBuilder().method(METHOD.GET).uri(uri.toString()).header("Version", getVersion());

      requestBuilder
          .encoder(new Encoder<Delegate, Reader>() { // Do not change this, wasync doesn't like lambdas
            @Override
            public Reader encode(Delegate s) {
              return new StringReader(JsonUtils.asJson(s));
            }
          })
          .transport(TRANSPORT.WEBSOCKET);
      return requestBuilder;
    } catch (URISyntaxException e) {
      throw new UnexpectedException("Unable to prepare uri", e);
    }
  }

  /**
   * This is just a workAround. We need to make sure, when delegate.sh is baked into container, its initialized with
   * proper value. We need to update watcher / manager communication for that. Its a bigger change, and will be handled
   * in a separate JIRA
   */
  private boolean isPollingForTasksEnabled() {
    if (isEcsDelegate()) {
      return "true".equals(System.getenv("POLL_FOR_TASKS"));
    }

    return delegateConfiguration.isPollForTasks();
  }

  private void logProxyConfiguration() {
    String proxyHost = System.getProperty("https.proxyHost");

    if (isBlank(proxyHost)) {
      log.info("No proxy settings. Configure in proxy.config if needed");
      return;
    }

    String proxyScheme = System.getProperty("proxyScheme");
    String proxyPort = System.getProperty("https.proxyPort");
    log.info("Using {} proxy {}:{}", proxyScheme, proxyHost, proxyPort);
    String nonProxyHostsString = System.getProperty("http.nonProxyHosts");

    if (nonProxyHostsString == null || isBlank(nonProxyHostsString)) {
      return;
    }

    String[] suffixes = nonProxyHostsString.split("\\|");
    List<String> nonProxyHosts = Stream.of(suffixes).map(suffix -> suffix.substring(1)).collect(toList());
    log.info("No proxy for hosts with suffix in: {}", nonProxyHosts);
  }

  private void logCfCliConfiguration() {
    String cfCli6Path = delegateConfiguration.getCfCli6Path();
    if (isNoneBlank(cfCli6Path)) {
      log.info(format("Found custom CF CLI6 binary path: %s", cfCli6Path));
    }

    String cfCli7Path = delegateConfiguration.getCfCli7Path();
    if (isNoneBlank(cfCli7Path)) {
      log.info(format("Found custom CF CLI7 binary path: %s", cfCli7Path));
    }

    validateCfCliExists();
  }

  private void handleOpen(Object o) {
    log.info("Event:{}, message:[{}]", Event.OPEN.name(), o.toString());
  }

  private void handleClose(Object o) {
    log.info("Event:{}, message:[{}]", Event.CLOSE.name(), o.toString());
    // TODO(brett): Disabling the fallback to poll for tasks as it can cause too much traffic to ingress controller
    // pollingForTasks.set(true);
    if (!closingSocket.get() && reconnectingSocket.compareAndSet(false, true)) {
      try {
        trySocketReconnect();
      } finally {
        reconnectingSocket.set(false);
      }
    }
  }

  private void handleError(Exception e) {
    log.info("Event:{}, message:[{}]", Event.ERROR.name(), e.getMessage());
    if (reconnectingSocket.compareAndSet(false, true)) {
      try {
        if (e instanceof SSLException || e instanceof TransportNotSupported) {
          log.info("Reopening connection to manager");
          try {
            socket.close();
          } catch (Exception ex) {
            // Ignore
          }
          trySocketReconnect();
        } else if (e instanceof ConnectException) {
          log.warn("Failed to connect.");
          restartNeeded.set(true);
        } else if (e instanceof ConcurrentModificationException) {
          log.error("Concurrent modification exception. Ignoring.");
        } else {
          log.error("Exception: " + e.getMessage(), e);
          try {
            finalizeSocket();
          } catch (Exception ex) {
            // Ignore
          }
          restartNeeded.set(true);
        }
      } finally {
        reconnectingSocket.set(false);
      }
    }
  }

  private void trySocketReconnect() {
    try {
      FibonacciBackOff.executeForEver(() -> {
        RequestBuilder requestBuilder = prepareRequestBuilder();
        return socket.open(requestBuilder.build());
      });
    } catch (IOException ex) {
      log.error("Unable to open socket", ex);
    }
  }

  private void finalizeSocket() {
    closingSocket.set(true);
    socket.close();
  }

  private void handleMessageSubmit(String message) {
    if (log.isDebugEnabled()) {
      log.debug("^^MSG: " + message);
    }
    systemExecutor.submit(() -> handleMessage(message));
  }

  private void handleMessage(String message) {
    if (StringUtils.startsWith(message, "[X]")) {
      String receivedId;
      if (isEcsDelegate()) {
        int indexForToken = message.lastIndexOf(TOKEN);
        receivedId = message.substring(3, indexForToken); // Remove the "[X]
      } else {
        receivedId = message.substring(3);
      }
      if (delegateId.equals(receivedId)) {
        long now = clock.millis();
        log.info("Delegate {} received heartbeat response {} after sending. {} since last response.", receivedId,
            getDurationString(lastHeartbeatSentAt.get(), now), getDurationString(lastHeartbeatReceivedAt.get(), now));

        handleEcsDelegateSpecificMessage(message);

        lastHeartbeatReceivedAt.set(now);
      } else {
        log.info("Heartbeat response for another delegate received: {}", receivedId);
      }
    } else if (StringUtils.equals(message, SELF_DESTRUCT)) {
      initiateSelfDestruct();
    } else if (StringUtils.equals(message, SELF_DESTRUCT + delegateId)) {
      initiateSelfDestruct();
    } else if (StringUtils.startsWith(message, SELF_DESTRUCT)) {
      if (StringUtils.startsWith(message, SELF_DESTRUCT + delegateId + "-")) {
        int len = (SELF_DESTRUCT + delegateId + "-").length();
        if (message.substring(len).equals(delegateConnectionId)) {
          initiateSelfDestruct();
        }
      }
    } else if (StringUtils.equals(message, USE_CDN)) {
      setSwitchStorage(true);
    } else if (StringUtils.equals(message, USE_STORAGE_PROXY)) {
      setSwitchStorage(false);
    } else if (StringUtils.contains(message, UPDATE_PERPETUAL_TASK)) {
      updateTasks();
    } else if (StringUtils.startsWith(message, MIGRATE)) {
      migrate(StringUtils.substringAfter(message, MIGRATE));
    } else if (StringUtils.startsWith(message, JRE_VERSION)) {
      updateJreVersion(StringUtils.substringAfter(message, JRE_VERSION));
    } else if (StringUtils.contains(message, INVALID_TOKEN.name())) {
      log.warn("Delegate used invalid token. Self destruct procedure will be initiated.");
      initiateSelfDestruct();
    } else if (StringUtils.contains(message, EXPIRED_TOKEN.name())) {
      log.warn("Delegate used expired token. It will be frozen and drained.");
      freeze();
    } else if (StringUtils.contains(message, REVOKED_TOKEN.name())) {
      log.warn("Delegate used revoked token. It will be frozen and drained.");
      freeze();
    } else if (!StringUtils.equals(message, "X")) {
      log.info("Executing: Event:{}, message:[{}]", Event.MESSAGE.name(), message);
      try {
        DelegateTaskEvent delegateTaskEvent = JsonUtils.asObject(message, DelegateTaskEvent.class);
        try (TaskLogContext ignore = new TaskLogContext(delegateTaskEvent.getDelegateTaskId(), OVERRIDE_ERROR)) {
          if (delegateTaskEvent instanceof DelegateTaskAbortEvent) {
            abortDelegateTask((DelegateTaskAbortEvent) delegateTaskEvent);
          } else {
            dispatchDelegateTask(delegateTaskEvent);
          }
        }
      } catch (Throwable e) {
        log.error("Exception while decoding task", e);
      }
    }
  }

  private void stopGrpcService() {
    if (delegateConfiguration.isGrpcServiceEnabled() && restartableServiceManager.isRunning()) {
      grpcServiceExecutor.submit(() -> restartableServiceManager.stop());
    }
  }

  private void startGrpcService() {
    if (delegateConfiguration.isGrpcServiceEnabled() && acquireTasks.get() && !restartableServiceManager.isRunning()) {
      grpcServiceExecutor.submit(() -> {
        restartableServiceManager.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> restartableServiceManager.stop()));
      });
    }
  }

  private void updateTasks() {
    if (perpetualTaskWorker != null) {
      perpetualTaskWorker.updateTasks();
    }
  }

  private void updateJreVersion(String targetJreVersion) {
    if (!targetJreVersion.equals(migrateToJreVersion)) {
      log.info("JRE version different. Migrating to {}", targetJreVersion);
      delegateJreVersionChangedAt = clock.millis();
      migrateToJreVersion = targetJreVersion;
      sendJreInformationToWatcher = false;
    } else {
      sendJreInformationToWatcher = clock.millis() - delegateJreVersionChangedAt > DELEGATE_JRE_VERSION_TIMEOUT;
    }

    log.debug("Send info to watcher {}", sendJreInformationToWatcher);
  }

  @Override
  public void pause() {
    if (!delegateConfiguration.isPollForTasks()) {
      finalizeSocket();
    }
  }

  private void resume() {
    try {
      if (!delegateConfiguration.isPollForTasks()) {
        FibonacciBackOff.executeForEver(() -> {
          RequestBuilder requestBuilder = prepareRequestBuilder();
          return socket.open(requestBuilder.build());
        });
      }
      if (perpetualTaskWorker != null) {
        perpetualTaskWorker.start();
      }
      upgradePending.set(false);
      upgradeNeeded.set(false);
      restartNeeded.set(false);
      acquireTasks.set(true);
    } catch (IOException e) {
      log.error("Failed to resume.", e);
      stop();
    }
  }

  @Override
  public void stop() {
    synchronized (waiter) {
      waiter.set(false);
      waiter.notifyAll();
    }
  }

  private <T> T delegateExecute(Call<T> call) throws IOException {
    Response<T> response = null;
    try {
      response = call.execute();
      return response.body();
    } finally {
      if (response != null && !response.isSuccessful()) {
        String errorResponse = response.errorBody().string();
        if (errorResponse.contains(INVALID_TOKEN.name())) {
          log.warn("Delegate used invalid token. Self destruct procedure will be initiated.");
          initiateSelfDestruct();
        } else if (errorResponse.contains(format(DUPLICATE_DELEGATE_ERROR_MESSAGE, delegateId, delegateConnectionId))) {
          initiateSelfDestruct();
        } else if (errorResponse.contains(EXPIRED_TOKEN.name())) {
          log.warn("Delegate used expired token. It will be frozen and drained.");
          freeze();
        } else if (errorResponse.contains(REVOKED_TOKEN.name()) || errorResponse.contains("Revoked Delegate Token")) {
          log.warn("Delegate used revoked token. It will be frozen and drained.");
          freeze();
        }
        response.errorBody().close();
      }
    }
  }

  private String registerDelegate(DelegateParamsBuilder builder) {
    updateBuilderIfEcsDelegate(builder);
    AtomicInteger attempts = new AtomicInteger(0);
    while (acquireTasks.get() && shouldContactManager()) {
      RestResponse<DelegateRegisterResponse> restResponse;
      try {
        attempts.incrementAndGet();
        String attemptString = attempts.get() > 1 ? " (Attempt " + attempts.get() + ")" : "";
        log.info("Registering delegate" + attemptString);
        DelegateParams delegateParams = builder.build()
                                            .toBuilder()
                                            .lastHeartBeat(clock.millis())
                                            .delegateType(DELEGATE_TYPE)
                                            .description(delegateConfiguration.getDescription())
                                            //.proxy(set to true if there is a system proxy)
                                            .pollingModeEnabled(delegateConfiguration.isPollForTasks())
                                            .ceEnabled(Boolean.parseBoolean(System.getenv("ENABlE_CE")))
                                            .build();
        restResponse = delegateExecute(delegateAgentManagerClient.registerDelegate(accountId, delegateParams));
      } catch (Exception e) {
        String msg = "Unknown error occurred while registering Delegate [" + accountId + "] with manager";
        log.error(msg, e);
        sleep(ofMinutes(1));
        continue;
      }
      if (restResponse == null || restResponse.getResource() == null) {
        log.error(
            "Error occurred while registering delegate with manager for account {}. Please see the manager log for more information",
            accountId);
        sleep(ofMinutes(1));
        continue;
      }

      DelegateRegisterResponse delegateResponse = restResponse.getResource();
      String responseDelegateId = delegateResponse.getDelegateId();
      handleEcsDelegateRegistrationResponse(delegateResponse);

      if (DelegateRegisterResponse.Action.SELF_DESTRUCT == delegateResponse.getAction()) {
        initiateSelfDestruct();
        sleep(ofMinutes(1));
        continue;
      }
      if (DelegateRegisterResponse.Action.MIGRATE == delegateResponse.getAction()) {
        migrate(delegateResponse.getMigrateUrl());
        continue;
      }
      builder.delegateId(responseDelegateId);
      log.info("Delegate registered with id {}", responseDelegateId);
      return responseDelegateId;
    }

    // Didn't register and not acquiring. Exiting.
    System.exit(1);
    return null;
  }

  private void startProfileCheck() {
    profileExecutor.scheduleWithFixedDelay(() -> {
      boolean forCodeFormattingOnly; // This line is here for clang-format
      synchronized (this) {
        checkForProfile();
      }
    }, 0, 3, TimeUnit.MINUTES);
  }

  private void checkForProfile() {
    if (shouldContactManager() && !executingProfile.get() && !isLocked(new File("profile")) && !frozen.get()) {
      try {
        log.info("Checking for profile ...");
        DelegateProfileParams profileParams = getProfile();
        boolean resultExists = new File("profile.result").exists();
        String profileId = profileParams == null ? "" : profileParams.getProfileId();
        long updated = profileParams == null || !resultExists ? 0L : profileParams.getProfileLastUpdatedAt();
        RestResponse<DelegateProfileParams> response = timeLimiter.callWithTimeout(
            ()
                -> delegateExecute(
                    delegateAgentManagerClient.checkForProfile(delegateId, accountId, profileId, updated)),
            15L, TimeUnit.SECONDS, true);
        if (response != null) {
          applyProfile(response.getResource());
        }
      } catch (UncheckedTimeoutException ex) {
        log.warn("Timed out checking for profile", ex);
      } catch (Exception e) {
        log.error("Error checking for profile", e);
      }
    }
  }

  private DelegateProfileParams getProfile() {
    File profile = new File("profile");
    if (profile.exists()) {
      try {
        return JsonUtils.asObject(FileUtils.readFileToString(profile, UTF_8), DelegateProfileParams.class);
      } catch (Exception e) {
        log.error("Error reading profile", e);
      }
    }
    return null;
  }

  private void applyProfile(DelegateProfileParams profile) {
    if (profile != null && executingProfile.compareAndSet(false, true)) {
      File profileFile = new File("profile");
      if (acquireLock(profileFile, ofMinutes(5))) {
        try {
          if ("NONE".equals(profile.getProfileId())) {
            FileUtils.deleteQuietly(profileFile);
            FileUtils.deleteQuietly(new File("profile.result"));
            return;
          }

          log.info("Updating delegate profile to [{} : {}], last update {} ...", profile.getProfileId(),
              profile.getName(), profile.getProfileLastUpdatedAt());
          String script = profile.getScriptContent();
          List<String> result = new ArrayList<>();
          int exitCode = 0;

          if (!isBlank(script)) {
            Logger scriptLogger = LoggerFactory.getLogger("delegate-profile-" + profile.getProfileId());
            scriptLogger.info("Executing profile script: {}", profile.getName());

            ProcessExecutor processExecutor = new ProcessExecutor()
                                                  .timeout(10, TimeUnit.MINUTES)
                                                  .command("/bin/bash", "-c", script)
                                                  .readOutput(true)
                                                  .redirectOutput(new LogOutputStream() {
                                                    @Override
                                                    protected void processLine(String line) {
                                                      scriptLogger.info(line);
                                                      result.add(line);
                                                    }
                                                  })
                                                  .redirectError(new LogOutputStream() {
                                                    @Override
                                                    protected void processLine(String line) {
                                                      scriptLogger.error(line);
                                                      result.add("ERROR: " + line);
                                                    }
                                                  });
            exitCode = processExecutor.execute().getExitValue();
          }

          saveProfile(profile, result);
          uploadProfileResult(exitCode);
          log.info("Profile applied");
        } catch (IOException e) {
          log.error("Error applying profile [{}]", profile.getName(), e);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
          log.info("Timed out", e);
        } catch (UncheckedTimeoutException ex) {
          log.error("Timed out sending profile result", ex);
        } catch (Exception e) {
          log.error("Error applying profile", e);
        } finally {
          executingProfile.set(false);
          if (!releaseLock(profileFile)) {
            log.error("Failed to release lock {}", profileFile.getPath());
          }
        }
      }
    }
  }

  private void saveProfile(DelegateProfileParams profile, List<String> result) {
    log.info("Saving profile result");
    try {
      File profileFile = new File("profile");
      if (profileFile.exists()) {
        FileUtils.forceDelete(profileFile);
      }
      FileUtils.touch(profileFile);
      FileUtils.write(profileFile, JsonUtils.asPrettyJson(profile), UTF_8);

      File resultFile = new File("profile.result");
      if (resultFile.exists()) {
        FileUtils.forceDelete(resultFile);
      }
      FileUtils.touch(resultFile);
      FileUtils.writeLines(resultFile, result);
    } catch (IOException e) {
      log.error("Error writing profile [{}]", profile.getName(), e);
    }
  }

  private void uploadProfileResult(int exitCode) throws Exception {
    log.info("Uploading profile result");
    // create RequestBody instance from file
    File profileResult = new File("profile.result");
    RequestBody requestFile = RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), profileResult);

    // MultipartBody.Part is used to send also the actual file name
    Part part = Part.createFormData("file", profileResult.getName(), requestFile);
    timeLimiter.callWithTimeout(()
                                    -> delegateExecute(delegateAgentManagerClient.saveProfileResult(
                                        delegateId, accountId, exitCode != 0, FileBucket.PROFILE_RESULTS, part)),
        15L, TimeUnit.SECONDS, true);
  }

  private void startInputCheck() {
    inputExecutor.scheduleWithFixedDelay(
        messageService.getMessageCheckingRunnable(TimeUnit.SECONDS.toMillis(2), message -> {
          if (UPGRADING_DELEGATE.equals(message.getMessage())) {
            upgradeNeeded.set(false);
          } else if (DELEGATE_STOP_ACQUIRING.equals(message.getMessage())) {
            handleStopAcquiringMessage(message.getFromProcess());
          } else if (DELEGATE_RESUME.equals(message.getMessage())) {
            resume();
          } else if (DELEGATE_SEND_VERSION_HEADER.equals(message.getMessage())) {
            DelegateAgentManagerClientFactory.setSendVersionHeader(Boolean.parseBoolean(message.getParams().get(0)));
            delegateAgentManagerClient = injector.getInstance(DelegateAgentManagerClient.class);
          } else if (DELEGATE_START_GRPC.equals(message.getMessage())) {
            startGrpcService();
          } else if (DELEGATE_STOP_GRPC.equals(message.getMessage())) {
            stopGrpcService();
          }
        }), 0, 1, TimeUnit.SECONDS);
  }

  public void freeze() {
    log.warn("Delegate with id: {} was put in freeze mode.", delegateId);
    frozenAt.set(System.currentTimeMillis());
    frozen.set(true);
  }

  private void handleStopAcquiringMessage(String sender) {
    log.info("Got stop-acquiring message from watcher {}", sender);
    if (acquireTasks.getAndSet(false)) {
      stoppedAcquiringAt = clock.millis();
      Map<String, Object> shutdownData = new HashMap<>();
      shutdownData.put(DELEGATE_SHUTDOWN_PENDING, true);
      shutdownData.put(DELEGATE_SHUTDOWN_STARTED, stoppedAcquiringAt);
      messageService.putAllData(DELEGATE_DASH + getProcessId(), shutdownData);

      systemExecutor.submit(() -> {
        long started = clock.millis();
        long now = started;
        while (!currentlyExecutingTasks.isEmpty() && now - started < UPGRADE_TIMEOUT) {
          sleep(ofSeconds(1));
          now = clock.millis();
          log.info("[Old] Completing {} tasks... ({} seconds elapsed): {}", currentlyExecutingTasks.size(),
              (now - started) / 1000L, currentlyExecutingTasks.keySet());
        }
        log.info(now - started < UPGRADE_TIMEOUT ? "[Old] Delegate finished with tasks. Pausing"
                                                 : "[Old] Timed out waiting to complete tasks. Pausing");
        signalService.pause();
        log.info("[Old] Shutting down");

        signalService.stop();
      });
      if (perpetualTaskWorker != null) {
        perpetualTaskWorker.stop();
      }
    }
  }

  private void startUpgradeCheck(String version) {
    if (!delegateConfiguration.isDoUpgrade()) {
      log.info("Auto upgrade is disabled in configuration");
      log.info("Delegate stays on version: [{}]", version);
      return;
    }

    log.info("Starting upgrade check at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    upgradeExecutor.scheduleWithFixedDelay(() -> {
      if (upgradePending.get()) {
        log.info("[Old] Upgrade is pending...");
      } else {
        log.info("Checking for upgrade");
        try {
          RestResponse<DelegateScripts> restResponse = timeLimiter.callWithTimeout(
              ()
                  -> delegateExecute(delegateAgentManagerClient.getDelegateScripts(accountId, version)),
              1L, TimeUnit.MINUTES, true);
          DelegateScripts delegateScripts = restResponse.getResource();
          if (delegateScripts.isDoUpgrade()) {
            upgradePending.set(true);

            upgradeStartedAt = clock.millis();
            Map<String, Object> upgradeData = new HashMap<>();
            upgradeData.put(DELEGATE_UPGRADE_PENDING, true);
            upgradeData.put(DELEGATE_UPGRADE_STARTED, upgradeStartedAt);
            messageService.putAllData(DELEGATE_DASH + getProcessId(), upgradeData);

            log.info("[Old] Replace run scripts");
            replaceRunScripts(delegateScripts);
            log.info("[Old] Run scripts downloaded. Upgrading delegate. Stop acquiring async tasks");
            upgradeVersion = delegateScripts.getVersion();
            upgradeNeeded.set(true);
          } else {
            log.info("Delegate up to date");
          }
        } catch (UncheckedTimeoutException tex) {
          log.warn("Timed out checking for upgrade", tex);
        } catch (Exception e) {
          upgradePending.set(false);
          upgradeNeeded.set(false);
          acquireTasks.set(true);
          log.error("Exception while checking for upgrade", e);
        }
      }
    }, 0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void startTaskPolling() {
    rescheduleExecutor.scheduleAtFixedRate(
        new Schedulable("Failed to poll for task", () -> taskPollExecutor.submit(this::pollForTask)), 0,
        POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    if (perpetualTaskWorker != null) {
      perpetualTaskWorker.start();
    }
  }

  private void pollForTask() {
    if (pollingForTasks.get() && shouldContactManager()) {
      try {
        DelegateTaskEventsResponse taskEventsResponse = timeLimiter.callWithTimeout(
            ()
                -> delegateExecute(delegateAgentManagerClient.pollTaskEvents(delegateId, accountId)),
            15L, TimeUnit.SECONDS, true);
        if (shouldProcessDelegateTaskEvents(taskEventsResponse)) {
          boolean processTaskEventsAsync = taskEventsResponse.isProcessTaskEventsAsync();
          List<DelegateTaskEvent> taskEvents = taskEventsResponse.getDelegateTaskEvents();
          if (processTaskEventsAsync) {
            log.info("Processing DelegateTaskEvents async {}", taskEvents);
            processDelegateTaskEventsAsync(taskEvents);
          } else {
            log.info("Processing DelegateTaskEvents {}", taskEvents);
            processDelegateTaskEventsInBlockingLoop(taskEvents);
          }
        }
      } catch (UncheckedTimeoutException tex) {
        log.warn("Timed out fetching delegate task events", tex);
      } catch (InterruptedException ie) {
        log.warn("Delegate service is being shut down, this task is being interrupted.", ie);
      } catch (Exception e) {
        log.error("Exception while decoding task", e);
      }
    }
  }

  private boolean shouldProcessDelegateTaskEvents(DelegateTaskEventsResponse taskEventsResponse) {
    return taskEventsResponse != null && isNotEmpty(taskEventsResponse.getDelegateTaskEvents());
  }

  private void processDelegateTaskEventsAsync(List<DelegateTaskEvent> taskEvents) {
    taskEvents.forEach(this::submitTaskEventForExecution);
  }

  private void processDelegateTaskEventsInBlockingLoop(List<DelegateTaskEvent> taskEvents) {
    taskEvents.forEach(this::processDelegateTaskEvent);
  }

  private void submitTaskEventForExecution(DelegateTaskEvent taskEvent) {
    asyncTaskDispatchExecutor.submit(() -> processDelegateTaskEvent(taskEvent));
  }

  private void processDelegateTaskEvent(DelegateTaskEvent taskEvent) {
    try (TaskLogContext ignore = new TaskLogContext(taskEvent.getDelegateTaskId(), OVERRIDE_ERROR)) {
      if (taskEvent instanceof DelegateTaskAbortEvent) {
        abortDelegateTask((DelegateTaskAbortEvent) taskEvent);
      } else {
        dispatchDelegateTask(taskEvent);
      }
    }
  }

  private void startHeartbeat(DelegateParamsBuilder builder, Socket socket) {
    log.info("Starting heartbeat at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    heartbeatExecutor.scheduleAtFixedRate(() -> {
      try {
        systemExecutor.submit(() -> {
          try {
            sendHeartbeat(builder, socket);
          } catch (Exception ex) {
            log.error("Exception while sending heartbeat", ex);
          }
        });
      } catch (Exception e) {
        log.error("Exception while scheduling heartbeat", e);
      }
    }, 0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void startKeepAlivePacket(DelegateParamsBuilder builder, Socket socket) {
    if (!isEcsDelegate()) {
      return;
    }

    // Only perform for ECS delegate.
    log.info("Starting KeepAlive Packet at interval {} ms", KEEP_ALIVE_INTERVAL);
    heartbeatExecutor.scheduleAtFixedRate(() -> {
      try {
        systemExecutor.submit(() -> {
          try {
            sendKeepAlivePacket(builder, socket);
          } catch (Exception ex) {
            log.error("Exception while sending KeepAlive Packet", ex);
          }
        });
      } catch (Exception e) {
        log.error("Exception while scheduling KeepAlive Packet", e);
      }
    }, 0, KEEP_ALIVE_INTERVAL, TimeUnit.MILLISECONDS);
  }

  private void startHeartbeatWhenPollingEnabled(DelegateParamsBuilder builder) {
    log.info("Starting heartbeat at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    heartbeatExecutor.scheduleAtFixedRate(() -> {
      if (pollingForTasks.get()) {
        try {
          systemExecutor.submit(() -> {
            try {
              sendHeartbeatWhenPollingEnabled(builder);
            } catch (Exception ex) {
              log.error("Exception while sending heartbeat", ex);
            }
          });
        } catch (Exception e) {
          log.error("Exception while scheduling heartbeat", e);
        }
      }
    }, 0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void startKeepAliveRequestWhenPollingEnabled(DelegateParamsBuilder builder) {
    log.info("Starting Keep Alive Request at interval {} ms", KEEP_ALIVE_INTERVAL);
    heartbeatExecutor.scheduleAtFixedRate(() -> {
      if (pollingForTasks.get() && isEcsDelegate()) {
        try {
          systemExecutor.submit(() -> {
            try {
              sendKeepAliveRequestWhenPollingEnabled(builder);
            } catch (Exception ex) {
              log.error("Exception while sending Keep Alive Request: ", ex);
            }
          });
        } catch (Exception e) {
          log.error("Exception while scheduling Keep Alive Request: ", e);
        }
      }
    }, 0, KEEP_ALIVE_INTERVAL, TimeUnit.MILLISECONDS);
  }

  private void startLocalHeartbeat() {
    localHeartbeatExecutor.scheduleAtFixedRate(() -> {
      try {
        systemExecutor.submit(() -> {
          Map<String, Object> statusData = new HashMap<>();
          if (selfDestruct.get()) {
            statusData.put(DELEGATE_SELF_DESTRUCT, true);
          } else {
            statusData.put(DELEGATE_HEARTBEAT, clock.millis());
            statusData.put(DELEGATE_VERSION, getVersionWithPatch());
            statusData.put(DELEGATE_IS_NEW, false);
            statusData.put(DELEGATE_RESTART_NEEDED, doRestartDelegate());
            statusData.put(DELEGATE_UPGRADE_NEEDED, upgradeNeeded.get());
            statusData.put(DELEGATE_UPGRADE_PENDING, upgradePending.get());
            statusData.put(DELEGATE_SHUTDOWN_PENDING, !acquireTasks.get());
            if (switchStorage.get() && !switchStorageMsgSent) {
              statusData.put(DELEGATE_SWITCH_STORAGE, TRUE);
              switchStorageMsgSent = true;
            }
            if (sendJreInformationToWatcher) {
              log.debug("Sending Delegate JRE: {} MigrateTo JRE: {} to watcher", System.getProperty(JAVA_VERSION),
                  migrateToJreVersion);
              statusData.put(DELEGATE_JRE_VERSION, System.getProperty(JAVA_VERSION));
              statusData.put(MIGRATE_TO_JRE_VERSION, migrateToJreVersion);
            }
            if (upgradePending.get()) {
              statusData.put(DELEGATE_UPGRADE_STARTED, upgradeStartedAt);
            }
            if (!acquireTasks.get()) {
              statusData.put(DELEGATE_SHUTDOWN_STARTED, stoppedAcquiringAt);
            }
            if (isNotBlank(migrateTo)) {
              statusData.put(DELEGATE_MIGRATE, migrateTo);
            }
          }
          messageService.putAllData(DELEGATE_DASH + getProcessId(), statusData);
          watchWatcher();
        });
      } catch (Exception e) {
        log.error("Exception while scheduling local heartbeat", e);
      }
      logCurrentTasks();
    }, 0, 10, TimeUnit.SECONDS);
  }

  private void watchWatcher() {
    long watcherHeartbeat =
        Optional.ofNullable(messageService.getData(WATCHER_DATA, WATCHER_HEARTBEAT, Long.class)).orElse(clock.millis());
    boolean heartbeatTimedOut = clock.millis() - watcherHeartbeat > WATCHER_HEARTBEAT_TIMEOUT;
    if (heartbeatTimedOut) {
      log.warn("Watcher heartbeat not seen for {} seconds", WATCHER_HEARTBEAT_TIMEOUT / 1000L);
    }
    String watcherVersion = messageService.getData(WATCHER_DATA, WATCHER_VERSION, String.class);
    String expectedVersion = findExpectedWatcherVersion();
    if (StringUtils.equals(expectedVersion, watcherVersion)) {
      watcherVersionMatchedAt = clock.millis();
    }
    boolean versionMatchTimedOut = clock.millis() - watcherVersionMatchedAt > WATCHER_VERSION_MATCH_TIMEOUT;
    if (versionMatchTimedOut) {
      log.warn("Watcher version mismatched for {} seconds. Version is {} but should be {}",
          WATCHER_VERSION_MATCH_TIMEOUT / 1000L, watcherVersion, expectedVersion);
    }

    boolean multiVersionRestartNeeded =
        multiVersion && clock.millis() - startTime > WATCHER_VERSION_MATCH_TIMEOUT && !new File(getVersion()).exists();

    if (heartbeatTimedOut || versionMatchTimedOut
        || (multiVersionRestartNeeded && multiVersionWatcherStarted.compareAndSet(false, true))) {
      String watcherProcess = messageService.getData(WATCHER_DATA, WATCHER_PROCESS, String.class);
      log.warn("Watcher process {} needs restart", watcherProcess);
      systemExecutor.submit(() -> {
        try {
          ProcessControl.ensureKilled(watcherProcess, Duration.ofSeconds(120));
          messageService.closeChannel(WATCHER, watcherProcess);
          sleep(ofSeconds(2));
          // Prevent a second restart attempt right away at next heartbeat by writing the watcher heartbeat and
          // resetting version matched timestamp
          messageService.putData(WATCHER_DATA, WATCHER_HEARTBEAT, clock.millis());
          watcherVersionMatchedAt = clock.millis();
          StartedProcess newWatcher = new ProcessExecutor()
                                          .command("nohup", "./start.sh")
                                          .redirectError(Slf4jStream.of("RestartWatcherScript").asError())
                                          .redirectOutput(Slf4jStream.of("RestartWatcherScript").asInfo())
                                          .readOutput(true)
                                          .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
                                          .start();
          if (multiVersionRestartNeeded && newWatcher.getProcess().isAlive()) {
            sleep(ofSeconds(20L));
            FileUtils.forceDelete(new File("delegate.sh"));
            FileUtils.forceDelete(new File("delegate.jar"));
            restartNeeded.set(true);
          }
        } catch (Exception e) {
          log.error("Error restarting watcher {}", watcherProcess, e);
        }
      });
    }
  }

  private String findExpectedWatcherVersion() {
    try {
      // TODO - if multiVersion, get versions from manager endpoint
      String watcherMetadata = Http.getResponseStringFromUrl(delegateConfiguration.getWatcherCheckLocation(), 10, 10);
      return substringBefore(watcherMetadata, " ").trim();
    } catch (IOException e) {
      log.warn("Unable to fetch watcher version information", e);
      return null;
    }
  }

  private boolean doRestartDelegate() {
    long now = clock.millis();

    boolean heartbeatExpired = ((now - lastHeartbeatSentAt.get()) > HEARTBEAT_TIMEOUT)
        || ((now - lastHeartbeatReceivedAt.get()) > HEARTBEAT_TIMEOUT);
    boolean freezeIntervalExpired = (now - frozenAt.get()) > FROZEN_TIMEOUT;

    return new File(START_SH).exists()
        && (restartNeeded.get() || (!frozen.get() && heartbeatExpired) || (frozen.get() && freezeIntervalExpired));
  }

  private void sendHeartbeat(DelegateParamsBuilder builder, Socket socket) {
    if (!shouldContactManager() || !acquireTasks.get() || frozen.get()) {
      return;
    }

    if (socket.status() == STATUS.OPEN || socket.status() == STATUS.REOPENED) {
      log.info("Sending heartbeat...");

      // This will Add ECS delegate specific fields if DELEGATE_TYPE = "ECS"
      updateBuilderIfEcsDelegate(builder);
      DelegateParams delegateParams =
          builder.build()
              .toBuilder()
              .lastHeartBeat(clock.millis())
              .pollingModeEnabled(delegateConfiguration.isPollForTasks())
              .currentlyExecutingDelegateTasks(currentlyExecutingTasks.values()
                                                   .stream()
                                                   .map(DelegateTaskPackage::getDelegateTaskId)
                                                   .collect(toList()))
              .location(Paths.get("").toAbsolutePath().toString())
              .build();

      try {
        timeLimiter.callWithTimeout(() -> socket.fire(JsonUtils.asJson(delegateParams)), 15L, TimeUnit.SECONDS, true);
        lastHeartbeatSentAt.set(clock.millis());
      } catch (UncheckedTimeoutException ex) {
        log.warn("Timed out sending heartbeat", ex);
      } catch (Exception e) {
        log.error("Error sending heartbeat", e);
      }
    } else {
      log.warn("Socket is not open");
    }
  }

  private void sendKeepAlivePacket(DelegateParamsBuilder builder, Socket socket) {
    if (!shouldContactManager()) {
      return;
    }

    if (socket.status() == STATUS.OPEN || socket.status() == STATUS.REOPENED) {
      log.info("Sending keepAlive packet...");
      updateBuilderIfEcsDelegate(builder);
      try {
        timeLimiter.callWithTimeout(() -> {
          DelegateParams delegateParams = builder.build().toBuilder().keepAlivePacket(true).build();
          return socket.fire(JsonUtils.asJson(delegateParams));
        }, 15L, TimeUnit.SECONDS, true);
      } catch (UncheckedTimeoutException ex) {
        log.warn("Timed out sending keep alive packet", ex);
      } catch (Exception e) {
        log.error("Error sending heartbeat", e);
      }
    } else {
      log.warn("Socket is not open");
    }
  }

  private void sendHeartbeatWhenPollingEnabled(DelegateParamsBuilder builder) {
    if (!shouldContactManager() || !acquireTasks.get() || frozen.get()) {
      return;
    }

    log.info("Sending heartbeat...");

    try {
      updateBuilderIfEcsDelegate(builder);
      DelegateParams delegateParams =
          builder.build()
              .toBuilder()
              .keepAlivePacket(false)
              .pollingModeEnabled(true)
              .currentlyExecutingDelegateTasks(currentlyExecutingTasks.values()
                                                   .stream()
                                                   .map(DelegateTaskPackage::getDelegateTaskId)
                                                   .collect(toList()))
              .location(Paths.get("").toAbsolutePath().toString())
              .build();
      lastHeartbeatSentAt.set(clock.millis());

      RestResponse<DelegateHeartbeatResponse> delegateParamsResponse =
          delegateExecute(delegateAgentManagerClient.delegateHeartbeat(accountId, delegateParams));
      long now = clock.millis();
      log.info("Delegate {} received heartbeat response {} after sending. {} since last response.", delegateId,
          getDurationString(lastHeartbeatSentAt.get(), now), getDurationString(lastHeartbeatReceivedAt.get(), now));
      lastHeartbeatReceivedAt.set(now);

      DelegateHeartbeatResponse receivedDelegateResponse = delegateParamsResponse.getResource();

      if (delegateId.equals(receivedDelegateResponse.getDelegateId())) {
        if (DelegateInstanceStatus.DELETED == DelegateInstanceStatus.valueOf(receivedDelegateResponse.getStatus())) {
          initiateSelfDestruct();
        } else {
          builder.delegateId(receivedDelegateResponse.getDelegateId());
        }
        lastHeartbeatSentAt.set(clock.millis());
        lastHeartbeatReceivedAt.set(clock.millis());
        updateTokenAndSeqNumFromPollingResponse(
            receivedDelegateResponse.getDelegateRandomToken(), receivedDelegateResponse.getSequenceNumber());
      }

      setSwitchStorage(receivedDelegateResponse.isUseCdn());
      updateJreVersion(receivedDelegateResponse.getJreVersion());

      timeLimiter.callWithTimeout(()
                                      -> delegateExecute(delegateAgentManagerClient.doConnectionHeartbeat(
                                          delegateId, accountId, connectionHeartbeat)),
          15L, TimeUnit.SECONDS, true);
      lastHeartbeatSentAt.set(clock.millis());

    } catch (UncheckedTimeoutException ex) {
      log.warn("Timed out sending heartbeat", ex);
    } catch (Exception e) {
      log.error("Error sending heartbeat", e);
    }
  }

  private void setSwitchStorage(boolean useCdn) {
    boolean usingCdn = delegateConfiguration.isUseCdn();
    if (usingCdn != useCdn) {
      switchStorage.set(true);
    }
  }

  private void sendKeepAliveRequestWhenPollingEnabled(DelegateParamsBuilder builder) {
    if (!shouldContactManager()) {
      return;
    }

    log.info("Sending Keep Alive Request...");
    try {
      updateBuilderIfEcsDelegate(builder);
      DelegateParams delegateParams =
          builder.build().toBuilder().keepAlivePacket(true).pollingModeEnabled(true).build();
      delegateExecute(delegateAgentManagerClient.registerDelegate(accountId, delegateParams));
    } catch (UncheckedTimeoutException ex) {
      log.warn("Timed out sending Keep Alive Request", ex);
    } catch (Exception e) {
      log.error("Error sending Keep Alive Request", e);
    }
  }

  private void updateTokenAndSeqNumFromPollingResponse(String delegateRandomToken, String sequenceNumber) {
    if (isEcsDelegate()) {
      handleEcsDelegateSpecificMessage(TOKEN + delegateRandomToken + SEQ + sequenceNumber);
    }
  }

  @Getter(lazy = true)
  private final Map<String, ThreadPoolExecutor> logExecutors =
      NullSafeImmutableMap.builder()
          .putIfNotNull("systemExecutor", (ThreadPoolExecutor) systemExecutor)
          .putIfNotNull("asyncExecutor", (ThreadPoolExecutor) asyncExecutor)
          .putIfNotNull("artifactExecutor", (ThreadPoolExecutor) artifactExecutor)
          .putIfNotNull("timeoutEnforcement", (ThreadPoolExecutor) timeoutEnforcement)
          .putIfNotNull("taskPollExecutor", (ThreadPoolExecutor) taskPollExecutor)
          .build();

  public Map<String, String> obtainPerformance() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    builder.put("maxValidatingTasksCount", Integer.toString(maxValidatingTasksCount.getAndSet(0)));
    builder.put("maxValidatingFuturesCount", Integer.toString(maxValidatingFuturesCount.getAndSet(0)));
    builder.put("maxExecutingTasksCount", Integer.toString(maxExecutingTasksCount.getAndSet(0)));
    builder.put("maxExecutingFuturesCount", Integer.toString(maxExecutingFuturesCount.getAndSet(0)));

    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    builder.put("cpu-process", Double.toString(Precision.round(osBean.getProcessCpuLoad() * 100, 2)));
    builder.put("cpu-system", Double.toString(Precision.round(osBean.getSystemCpuLoad() * 100, 2)));

    for (Entry<String, ThreadPoolExecutor> executorEntry : getLogExecutors().entrySet()) {
      builder.put(executorEntry.getKey(), Integer.toString(executorEntry.getValue().getActiveCount()));
    }
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    memoryUsage(builder, "heap-", memoryMXBean.getHeapMemoryUsage());

    memoryUsage(builder, "non-heap-", memoryMXBean.getNonHeapMemoryUsage());

    return builder.build();
  }

  private void logCurrentTasks() {
    try (AutoLogContext ignore = new AutoLogContext(obtainPerformance(), OVERRIDE_NESTS)) {
      log.info("Current performance");
    }
  }

  private void memoryUsage(ImmutableMap.Builder<String, String> builder, String prefix, MemoryUsage memoryUsage) {
    builder.put(prefix + "init", Long.toString(memoryUsage.getInit()));
    builder.put(prefix + "used", Long.toString(memoryUsage.getUsed()));
    builder.put(prefix + "committed", Long.toString(memoryUsage.getCommitted()));
    builder.put(prefix + "max", Long.toString(memoryUsage.getMax()));
  }

  private void abortDelegateTask(DelegateTaskAbortEvent delegateTaskEvent) {
    log.info("Aborting task {}", delegateTaskEvent);
    Optional.ofNullable(currentlyValidatingFutures.get(delegateTaskEvent.getDelegateTaskId()))
        .ifPresent(future -> future.cancel(true));
    currentlyValidatingTasks.remove(delegateTaskEvent.getDelegateTaskId());
    currentlyValidatingFutures.remove(delegateTaskEvent.getDelegateTaskId());
    log.info("Removed from validating futures on abort");

    Optional.ofNullable(currentlyExecutingFutures.get(delegateTaskEvent.getDelegateTaskId()))
        .ifPresent(future -> future.cancel(true));
    currentlyExecutingTasks.remove(delegateTaskEvent.getDelegateTaskId());
    if (currentlyExecutingFutures.remove(delegateTaskEvent.getDelegateTaskId()) != null) {
      log.info("Removed from executing futures on abort");
    }
  }

  private void dispatchDelegateTask(DelegateTaskEvent delegateTaskEvent) {
    if (!shouldContactManager()) {
      return;
    }

    log.info("DelegateTaskEvent received - {}", delegateTaskEvent);

    String delegateTaskId = delegateTaskEvent.getDelegateTaskId();
    if (delegateTaskId == null) {
      log.warn("Delegate task id cannot be null");
      return;
    }

    if (frozen.get()) {
      log.info(
          "Delegate process with detected time out of sync or with revoked token is running. Won't acquire tasks.");
      return;
    }

    if (!acquireTasks.get()) {
      log.info("[Old] Upgraded process is running. Won't acquire task while completing other tasks");
      return;
    }

    if (upgradePending.get() && !delegateTaskEvent.isSync()) {
      log.info("[Old] Upgrade pending, won't acquire async task");
      return;
    }

    if (currentlyAcquiringTasks.contains(delegateTaskId)) {
      if (log.isDebugEnabled()) {
        log.debug("Task [DelegateTaskEvent: {}] currently acquiring. Don't acquire again", delegateTaskEvent);
      }
      return;
    }

    if (currentlyValidatingTasks.containsKey(delegateTaskId)) {
      if (log.isDebugEnabled()) {
        log.debug("Task [DelegateTaskEvent: {}] already validating. Don't validate again", delegateTaskEvent);
      }
      return;
    }

    if (currentlyExecutingTasks.containsKey(delegateTaskId)) {
      if (log.isDebugEnabled()) {
        log.debug("Task [DelegateTaskEvent: {}] already acquired. Don't acquire again", delegateTaskEvent);
      }
      return;
    }

    try {
      int perpetualTaskCount = 0;
      if (perpetualTaskWorker != null) {
        perpetualTaskCount = perpetualTaskWorker.getCurrentlyExecutingPerpetualTasksCount().intValue();
      }

      if (delegateTaskLimit > 0
          && (currentlyExecutingTasks.size() + currentlyValidatingTasks.size() + perpetualTaskCount)
              >= delegateTaskLimit) {
        log.info("Delegate reached Delegate Size Task Limit of {}. It will not acquire this time.", delegateTaskLimit);
        return;
      }

      currentlyAcquiringTasks.add(delegateTaskId);

      // Delay response if already working on many tasks
      sleep(ofMillis(100 * Math.min(currentlyExecutingTasks.size() + currentlyValidatingTasks.size(), 10)));

      log.info("Try to acquire DelegateTask - accountId: {}", accountId);

      DelegateTaskPackage delegateTaskPackage =
          delegateExecute(delegateAgentManagerClient.acquireTask(delegateId, delegateTaskId, accountId));
      if (delegateTaskPackage == null || delegateTaskPackage.getData() == null) {
        log.info("Delegate task data not available - accountId: {}", delegateTaskEvent.getAccountId());
        return;
      }

      TaskData taskData = delegateTaskPackage.getData();
      if (isEmpty(delegateTaskPackage.getDelegateId())) {
        // Not whitelisted. Perform validation.
        // TODO: Remove this once TaskValidation does not use secrets

        // applyDelegateSecretFunctor(delegatePackage);
        DelegateValidateTask delegateValidateTask = getDelegateValidateTask(delegateTaskEvent, delegateTaskPackage);
        injector.injectMembers(delegateValidateTask);
        currentlyValidatingTasks.put(delegateTaskPackage.getDelegateTaskId(), delegateTaskPackage);
        updateCounterIfLessThanCurrent(maxValidatingTasksCount, currentlyValidatingTasks.size());
        ExecutorService executorService = selectExecutorService(taskData);

        Future<List<DelegateConnectionResult>> future = executorService.submit(delegateValidateTask::validationResults);
        currentlyValidatingFutures.put(delegateTaskPackage.getDelegateTaskId(), future);

        updateCounterIfLessThanCurrent(maxValidatingFuturesCount, currentlyValidatingFutures.size());

      } else if (delegateId.equals(delegateTaskPackage.getDelegateId())) {
        applyDelegateSecretFunctor(delegateTaskPackage);
        // Whitelisted. Proceed immediately.
        log.info("Delegate {} whitelisted for task and accountId: {}", delegateId, accountId);
        executeTask(delegateTaskPackage);
      }
    } catch (IOException e) {
      log.error("Unable to get task for validation", e);
    } finally {
      currentlyAcquiringTasks.remove(delegateTaskId);
    }
  }

  private ExecutorService selectExecutorService(TaskData taskData) {
    if (taskData.isAsync()) {
      return asyncExecutor;
    }
    if (taskData.getTaskType().contains("BUILD")) {
      return artifactExecutor;
    }

    return syncExecutor;
  }

  @NotNull
  private List<String> getCapabilityDetails(DelegateTaskPackage delegateTaskPackage) {
    return delegateTaskPackage.getExecutionCapabilities()
        .stream()
        .map(executionCapability
            -> executionCapability.getCapabilityType().name() + ":" + executionCapability.fetchCapabilityBasis())
        .collect(toList());
  }

  private void logErrorDetails(TaskData taskData, List<DelegateConnectionResult> alternativeResults, boolean original) {
    List<String> resultDetails = alternativeResults.stream()
                                     .map(result -> result.getCriteria() + ":" + result.isValidated())
                                     .collect(Collectors.toList());
    log.error(
        "[DelegateCapability] The original validation {} is different from the alternative for task type {}. Result Details for capability are {} ",
        original, taskData.getTaskType(), HarnessStringUtils.join("|", resultDetails));
    if (taskData.getTaskType().equals(TaskType.COMMAND.name())) {
      CommandExecutionContext commandExecutionContext = (CommandExecutionContext) taskData.getParameters()[1];
      log.error("[DelegateCapability] CommandExecution context has deployment type {}",
          commandExecutionContext.getDeploymentType());
    }
  }

  private DelegateValidateTask getDelegateValidateTask(
      DelegateTaskEvent delegateTaskEvent, DelegateTaskPackage delegateTaskPackage) {
    Consumer<List<DelegateConnectionResult>> postValidationFunction =
        getPostValidationFunction(delegateTaskEvent, delegateTaskPackage.getDelegateTaskId());

    return new CapabilityCheckController(delegateId, delegateTaskPackage, postValidationFunction);
  }

  private Consumer<List<DelegateConnectionResult>> getPostValidationFunction(
      DelegateTaskEvent delegateTaskEvent, String taskId) {
    return delegateConnectionResults -> {
      try (AutoLogContext logContext = new TaskLogContext(taskId, OVERRIDE_ERROR)) {
        currentlyValidatingTasks.remove(taskId);
        currentlyValidatingFutures.remove(taskId);
        log.info("Removed from validating futures on post validation");
        List<DelegateConnectionResult> results = Optional.ofNullable(delegateConnectionResults).orElse(emptyList());
        boolean validated = results.stream().allMatch(DelegateConnectionResult::isValidated);
        log.info("Validation {} for task", validated ? "succeeded" : "failed");
        try {
          DelegateTaskPackage delegateTaskPackage =
              execute(delegateAgentManagerClient.reportConnectionResults(delegateId,
                  delegateTaskEvent.getDelegateTaskId(), accountId, getDelegateConnectionResultDetails(results)));

          if (delegateTaskPackage != null && delegateTaskPackage.getData() != null
              && delegateId.equals(delegateTaskPackage.getDelegateId())) {
            applyDelegateSecretFunctor(delegateTaskPackage);
            executeTask(delegateTaskPackage);
          } else {
            log.info("Did not get the go-ahead to proceed for task");
            if (validated) {
              log.info("Task validated but was not assigned");
            } else {
              int delay = POLL_INTERVAL_SECONDS + 3;
              log.info("Waiting {} seconds to give other delegates a chance to validate task", delay);
              sleep(ofSeconds(delay));
              try {
                log.info("Manager check whether to fail task");
                execute(delegateAgentManagerClient.failIfAllDelegatesFailed(
                    delegateId, delegateTaskEvent.getDelegateTaskId(), accountId));
              } catch (IOException e) {
                log.error("Unable to tell manager to check whether to fail for task", e);
              }
            }
          }
        } catch (IOException e) {
          log.error("Unable to report validation results for task", e);
        }
      }
    };
  }

  private List<DelegateConnectionResultDetail> getDelegateConnectionResultDetails(
      List<DelegateConnectionResult> results) {
    List<DelegateConnectionResultDetail> delegateConnectionResultDetails = new ArrayList<>();
    for (DelegateConnectionResult source : results) {
      DelegateConnectionResultDetail target = DelegateConnectionResultDetail.builder().build();
      target.setAccountId(source.getAccountId());
      target.setCriteria(source.getCriteria());
      target.setDelegateId(source.getDelegateId());
      target.setDuration(source.getDuration());
      target.setLastUpdatedAt(source.getLastUpdatedAt());
      target.setUuid(source.getUuid());
      target.setValidated(source.isValidated());
      target.setValidUntil(source.getValidUntil());
      delegateConnectionResultDetails.add(target);
    }
    return delegateConnectionResultDetails;
  }

  private void executeTask(@NotNull DelegateTaskPackage delegateTaskPackage) {
    TaskData taskData = delegateTaskPackage.getData();

    if (currentlyExecutingTasks.containsKey(delegateTaskPackage.getDelegateTaskId())) {
      log.info("Already executing task");
      return;
    }

    log.info("DelegateTask acquired - accountId: {}, taskType: {}", accountId, taskData.getTaskType());
    Pair<String, Set<String>> activitySecrets = obtainActivitySecrets(delegateTaskPackage);
    Optional<LogSanitizer> sanitizer = getLogSanitizer(activitySecrets);
    ILogStreamingTaskClient logStreamingTaskClient = getLogStreamingTaskClient(activitySecrets, delegateTaskPackage);

    DelegateRunnableTask delegateRunnableTask = delegateTaskFactory.getDelegateRunnableTask(
        TaskType.valueOf(taskData.getTaskType()), delegateTaskPackage, logStreamingTaskClient,
        getPostExecutionFunction(
            delegateTaskPackage.getDelegateTaskId(), sanitizer.orElse(null), logStreamingTaskClient),
        getPreExecutionFunction(delegateTaskPackage, sanitizer.orElse(null), logStreamingTaskClient));
    if (delegateRunnableTask instanceof AbstractDelegateRunnableTask) {
      ((AbstractDelegateRunnableTask) delegateRunnableTask).setDelegateHostname(HOST_NAME);
    }
    injector.injectMembers(delegateRunnableTask);
    ExecutorService executorService = selectExecutorService(taskData);
    Future taskFuture = executorService.submit(delegateRunnableTask);
    log.info("Task future in executeTask: done:{}, cancelled:{}", taskFuture.isDone(), taskFuture.isCancelled());
    currentlyExecutingFutures.put(delegateTaskPackage.getDelegateTaskId(), taskFuture);
    updateCounterIfLessThanCurrent(maxExecutingFuturesCount, currentlyExecutingFutures.size());

    timeoutEnforcement.submit(() -> enforceDelegateTaskTimeout(delegateTaskPackage.getDelegateTaskId(), taskData));
    log.info("Task submitted for execution");
  }

  private ILogStreamingTaskClient getLogStreamingTaskClient(
      Pair<String, Set<String>> activitySecrets, DelegateTaskPackage delegateTaskPackage) {
    boolean logStreamingConfigPresent = false;
    boolean logCallbackConfigPresent = false;
    String appId = null;
    String activityId = null;

    if (logStreamingClient != null && !isBlank(delegateTaskPackage.getLogStreamingToken())
        && !isEmpty(delegateTaskPackage.getLogStreamingAbstractions())) {
      logStreamingConfigPresent = true;
    }

    // Extract appId and activityId from task params, in case LogCallback logging has to be used for backward
    // compatibility reasons
    Object[] taskParameters = delegateTaskPackage.getData().getParameters();
    if (taskParameters != null && taskParameters.length == 1 && taskParameters[0] instanceof Cd1ApplicationAccess
        && taskParameters[0] instanceof ActivityAccess) {
      Cd1ApplicationAccess applicationAccess = (Cd1ApplicationAccess) taskParameters[0];
      appId = applicationAccess.getAppId();

      ActivityAccess activityAccess = (ActivityAccess) taskParameters[0];
      activityId = activityAccess.getActivityId();
    }

    if (!isBlank(appId) && !isBlank(activityId)) {
      logCallbackConfigPresent = true;
    }

    if (!logStreamingConfigPresent && !logCallbackConfigPresent) {
      return null;
    }
    String logBaseKey = delegateTaskPackage.getLogStreamingAbstractions() != null
        ? LogStreamingHelper.generateLogBaseKey(delegateTaskPackage.getLogStreamingAbstractions())
        : EMPTY;

    LogStreamingTaskClientBuilder taskClientBuilder =
        LogStreamingTaskClient.builder()
            .logStreamingClient(logStreamingClient)
            .accountId(delegateTaskPackage.getAccountId())
            .token(delegateTaskPackage.getLogStreamingToken())
            .logStreamingSanitizer(LogStreamingSanitizer.builder().secrets(activitySecrets.getRight()).build())
            .baseLogKey(logBaseKey)
            .logService(delegateLogService)
            .appId(appId)
            .activityId(activityId);

    if (isNotBlank(delegateTaskPackage.getDelegateCallbackToken()) && delegateServiceGrpcAgentClient != null) {
      taskClientBuilder.taskProgressClient(TaskProgressClient.builder()
                                               .accountId(delegateTaskPackage.getAccountId())
                                               .taskId(delegateTaskPackage.getDelegateTaskId())
                                               .delegateCallbackToken(delegateTaskPackage.getDelegateCallbackToken())
                                               .delegateServiceGrpcAgentClient(delegateServiceGrpcAgentClient)
                                               .kryoSerializer(kryoSerializer)
                                               .build());
    }

    return taskClientBuilder.build();
  }

  private Optional<LogSanitizer> getLogSanitizer(Pair<String, Set<String>> activitySecrets) {
    // Create log sanitizer only if activityId and secrets are present
    if (isNotBlank(activitySecrets.getLeft()) && isNotEmpty(activitySecrets.getRight())) {
      return Optional.of(new ActivityBasedLogSanitizer(activitySecrets.getLeft(), activitySecrets.getRight()));
    } else {
      return Optional.empty();
    }
  }

  private Pair<String, Set<String>> obtainActivitySecrets(@NotNull DelegateTaskPackage delegateTaskPackage) {
    TaskData taskData = delegateTaskPackage.getData();

    String activityId = null;
    Set<String> secrets = new HashSet<>(delegateTaskPackage.getSecrets());

    // Add other system secrets
    addSystemSecrets(secrets);

    // TODO: This gets secrets for Shell Script, Shell Script Provision, and Command only
    // When secret decryption is moved to delegate for each task then those secrets can be used instead.
    Object[] parameters = taskData.getParameters();
    if (parameters.length == 1 && parameters[0] instanceof TaskParameters) {
      if (parameters[0] instanceof ActivityAccess) {
        activityId = ((ActivityAccess) parameters[0]).getActivityId();
      }

      if (parameters[0] instanceof ShellScriptParameters) {
        // Shell Script
        ShellScriptParameters shellScriptParameters = (ShellScriptParameters) parameters[0];
        secrets.addAll(secretsFromMaskedVariables(
            shellScriptParameters.getServiceVariables(), shellScriptParameters.getSafeDisplayServiceVariables()));
      } else if (parameters[0] instanceof ShellScriptProvisionParameters) {
        // Shell Script Provision
        ShellScriptProvisionParameters shellScriptProvisionParameters = (ShellScriptProvisionParameters) parameters[0];
        Map<String, EncryptedDataDetail> encryptedVariables = shellScriptProvisionParameters.getEncryptedVariables();
        if (isNotEmpty(encryptedVariables)) {
          for (Entry<String, EncryptedDataDetail> encryptedVariable : encryptedVariables.entrySet()) {
            secrets.add(String.valueOf(encryptionService.getDecryptedValue(encryptedVariable.getValue(), false)));
          }
        }
      } else if (parameters[0] instanceof CommandParameters) {
        // Command
        CommandParameters commandParameters = (CommandParameters) parameters[0];
        activityId = commandParameters.getActivityId();
        secrets.addAll(secretsFromMaskedVariables(
            commandParameters.getServiceVariables(), commandParameters.getSafeDisplayServiceVariables()));
      }
    } else {
      if (parameters.length >= 2 && parameters[0] instanceof Command
          && parameters[1] instanceof CommandExecutionContext) {
        // Command
        CommandExecutionContext context = (CommandExecutionContext) parameters[1];
        activityId = context.getActivityId();
        secrets.addAll(
            secretsFromMaskedVariables(context.getServiceVariables(), context.getSafeDisplayServiceVariables()));
      }
    }

    return Pair.of(activityId, secrets);
  }

  private void addSystemSecrets(Set<String> secrets) {
    // Add config file secrets
    secrets.add(delegateConfiguration.getAccountSecret());

    // Add environment variable secrets
    String delegateProfileId = System.getenv().get("DELEGATE_PROFILE");
    if (isNotBlank(delegateProfileId)) {
      secrets.add(delegateProfileId);
    }

    if (isNotBlank(delegateSessionIdentifier)) {
      secrets.add(delegateSessionIdentifier);
    }

    String proxyUser = System.getenv().get("PROXY_USER");
    if (isNotBlank(proxyUser)) {
      secrets.add(proxyUser);
    }

    String proxyPassword = System.getenv().get("PROXY_PASSWORD");
    if (isNotBlank(proxyPassword)) {
      secrets.add(proxyPassword);
    }
  }

  /**
   * Create set of secrets from two maps. Both contain all variables, secret and plain.
   * The first does not mask secrets while the second does
   *
   * @param serviceVariables            contains all variables, secret and plain, unmasked
   * @param safeDisplayServiceVariables contains all variables with secret ones masked
   * @return set of secret variable values, unmasked
   */
  private static Set<String> secretsFromMaskedVariables(
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables) {
    Set<String> secrets = new HashSet<>();
    if (isNotEmpty(serviceVariables) && isNotEmpty(safeDisplayServiceVariables)) {
      for (Map.Entry<String, String> entry : safeDisplayServiceVariables.entrySet()) {
        if (SECRET_MASK.equals(entry.getValue())) {
          secrets.add(serviceVariables.get(entry.getKey()));
        }
      }
    }
    return secrets;
  }

  private BooleanSupplier getPreExecutionFunction(@NotNull DelegateTaskPackage delegateTaskPackage,
      LogSanitizer sanitizer, ILogStreamingTaskClient logStreamingTaskClient) {
    return () -> {
      if (logStreamingTaskClient != null) {
        try {
          // Opens the log stream for task
          logStreamingTaskClient.openStream(null);
        } catch (Exception ex) {
          log.error("Unexpected error occurred while opening the log stream.");
        }
      }

      if (!currentlyExecutingTasks.containsKey(delegateTaskPackage.getDelegateTaskId())) {
        log.info("Adding task to executing tasks");
        currentlyExecutingTasks.put(delegateTaskPackage.getDelegateTaskId(), delegateTaskPackage);
        updateCounterIfLessThanCurrent(maxExecutingTasksCount, currentlyExecutingTasks.size());
        if (sanitizer != null) {
          delegateLogService.registerLogSanitizer(sanitizer);
        }
        return true;
      } else {
        log.info("Task is already being executed");
        return false;
      }
    };
  }

  private void updateCounterIfLessThanCurrent(AtomicInteger counter, int current) {
    counter.updateAndGet(value -> Math.max(value, current));
  }

  private Consumer<DelegateTaskResponse> getPostExecutionFunction(
      String taskId, LogSanitizer sanitizer, ILogStreamingTaskClient logStreamingTaskClient) {
    return taskResponse -> {
      if (logStreamingTaskClient != null) {
        try {
          // Closes the log stream for the task
          logStreamingTaskClient.closeStream(null);
        } catch (Exception ex) {
          log.error("Unexpected error occurred while closing the log stream.");
        }
      }

      Response<ResponseBody> response = null;
      try {
        response = timeLimiter.callWithTimeout(() -> {
          Response<ResponseBody> resp = null;
          int retries = 3;
          while (retries-- > 0) {
            resp = delegateAgentManagerClient.sendTaskStatus(delegateId, taskId, accountId, taskResponse).execute();
            if (resp != null && resp.code() >= 200 && resp.code() <= 299) {
              log.info("Task {} response sent to manager", taskId);
              return resp;
            } else {
              log.warn("Response received for sent task {}: {}. {}", taskId, resp == null ? "null" : resp.code(),
                  retries > 0 ? "Retrying." : "Giving up.");
              sleep(ofMillis(200));
            }
          }
          return resp;
        }, 30L, TimeUnit.SECONDS, true);
      } catch (UncheckedTimeoutException ex) {
        log.warn("Timed out sending response to manager", ex);
      } catch (Exception e) {
        log.error("Unable to send response to manager", e);
      } finally {
        if (sanitizer != null) {
          delegateLogService.unregisterLogSanitizer(sanitizer);
        }
        currentlyExecutingTasks.remove(taskId);
        if (currentlyExecutingFutures.remove(taskId) != null) {
          log.info("Removed from executing futures on post execution");
        }
        if (response != null && response.errorBody() != null && !response.isSuccessful()) {
          response.errorBody().close();
        }
        if (response != null && response.body() != null && response.isSuccessful()) {
          response.body().close();
        }
      }
    };
  }

  private void enforceDelegateTaskTimeout(String taskId, TaskData taskData) {
    long startingTime = clock.millis();
    boolean stillRunning = true;
    long timeout = taskData.getTimeout() + TimeUnit.SECONDS.toMillis(30L);
    Future taskFuture = null;
    while (stillRunning && clock.millis() - startingTime < timeout) {
      log.info("Task time remaining for {}: {} ms", taskId, startingTime + timeout - clock.millis());
      sleep(ofSeconds(5));
      taskFuture = currentlyExecutingFutures.get(taskId);
      if (taskFuture != null) {
        log.info("Task future: {} - done:{}, cancelled:{}", taskId, taskFuture.isDone(), taskFuture.isCancelled());
      }
      stillRunning = taskFuture != null && !taskFuture.isDone() && !taskFuture.isCancelled();
    }
    if (stillRunning) {
      log.error("Task {} timed out after {} milliseconds", taskId, timeout);
      Optional.ofNullable(currentlyExecutingFutures.get(taskId)).ifPresent(future -> future.cancel(true));
    }
    if (taskFuture != null) {
      try {
        timeLimiter.callWithTimeout(taskFuture::get, 5L, TimeUnit.SECONDS, true);
      } catch (UncheckedTimeoutException e) {
        ignoredOnPurpose(e);
        log.error("Timed out getting task future");
      } catch (CancellationException e) {
        ignoredOnPurpose(e);
        log.error("Task {} was cancelled", taskId);
      } catch (Exception e) {
        log.error("Error from task future {}", taskId, e);
      }
    }
    currentlyExecutingTasks.remove(taskId);
    if (currentlyExecutingFutures.remove(taskId) != null) {
      log.info("Removed {} from executing futures on timeout", taskId);
    }
  }

  private void replaceRunScripts(DelegateScripts delegateScripts) throws IOException {
    for (String fileName : asList(START_SH, "stop.sh", "delegate.sh", "setup-proxy.sh")) {
      Files.deleteIfExists(Paths.get(fileName));
      File scriptFile = new File(fileName);
      String script = delegateScripts.getScriptByName(fileName);

      if (isNotEmpty(script)) {
        try (BufferedWriter writer = Files.newBufferedWriter(scriptFile.toPath())) {
          writer.write(script, 0, script.length());
          writer.flush();
        }
        log.info("[Old] Done replacing file [{}]. Set User and Group permission", scriptFile);
        Files.setPosixFilePermissions(scriptFile.toPath(),
            Sets.newHashSet(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ));
        log.info("[Old] Done setting file permissions");
      } else {
        log.error("[Old] Script for file [{}] was not replaced", scriptFile);
      }
    }
  }

  private void cleanupOldDelegateVersionFromBackup() {
    try {
      cleanup(new File(System.getProperty("user.dir")), getVersion(), upgradeVersion, "backup.");
    } catch (Exception ex) {
      log.error("Failed to clean delegate version [{}] from Backup", upgradeVersion, ex);
    }
  }

  private void removeDelegateVersionFromCapsule() {
    try {
      cleanup(new File(System.getProperty("capsule.dir")).getParentFile(), getVersion(), upgradeVersion, "delegate-");
    } catch (Exception ex) {
      log.error("Failed to clean delegate version [{}] from Capsule", upgradeVersion, ex);
    }
  }

  private void cleanup(File dir, String currentVersion, String newVersion, String pattern) {
    FileUtils.listFilesAndDirs(dir, falseFileFilter(), FileFilterUtils.prefixFileFilter(pattern)).forEach(file -> {
      if (!dir.equals(file) && !file.getName().contains(currentVersion) && !file.getName().contains(newVersion)) {
        log.info("[Old] File Name to be deleted = " + file.getAbsolutePath());
        FileUtils.deleteQuietly(file);
      }
    });
  }

  private String getVersion() {
    return versionInfoManager.getVersionInfo().getVersion();
  }

  private String getVersionWithPatch() {
    if (versionInfoManager.getVersionInfo().getPatch() != null) {
      return versionInfoManager.getVersionInfo().getVersion() + "-" + versionInfoManager.getVersionInfo().getPatch();
    } else {
      return versionInfoManager.getVersionInfo().getVersion();
    }
  }

  private void initiateSelfDestruct() {
    log.info("Self destruct sequence initiated...");
    acquireTasks.set(false);
    upgradePending.set(false);
    upgradeNeeded.set(false);
    restartNeeded.set(false);
    selfDestruct.set(true);

    if (socket != null) {
      finalizeSocket();
    }

    DelegateStackdriverLogAppender.setManagerClient(null);
    if (perpetualTaskWorker != null) {
      perpetualTaskWorker.stop();
    }
  }

  private void migrate(String newUrl) {
    if (!newUrl.endsWith("/")) {
      newUrl = newUrl + "/";
    }
    migrateTo = newUrl;
  }

  private void handleEcsDelegateSpecificMessage(String message) {
    if (isEcsDelegate()) {
      int indexForToken = message.lastIndexOf(TOKEN);
      int indexForSeqNum = message.lastIndexOf(SEQ);
      String token = message.substring(indexForToken + 7, indexForSeqNum);
      String sequenceNum = message.substring(indexForSeqNum + 5);

      // Did not receive correct data, skip updating token and sequence
      if (isInvalidData(token) || isInvalidData(sequenceNum)) {
        return;
      }

      try {
        FileIo.writeWithExclusiveLockAcrossProcesses(
            TOKEN + token + SEQ + sequenceNum, DELEGATE_SEQUENCE_CONFIG_FILE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("Token Received From Manager : {}, SeqNum Received From Manager: {}", token, sequenceNum);
      } catch (Exception e) {
        log.error("Failed to write registration response into delegate_sequence file", e);
      }
    }
  }

  private boolean isInvalidData(String value) {
    return isBlank(value) || "null".equalsIgnoreCase(value);
  }

  private void updateBuilderIfEcsDelegate(DelegateParamsBuilder builder) {
    if (!isEcsDelegate()) {
      return;
    }

    builder.delegateGroupName(DELEGATE_GROUP_NAME);

    try {
      if (!FileIo.checkIfFileExist(DELEGATE_SEQUENCE_CONFIG_FILE)) {
        generateEcsDelegateSequenceConfigFile();
      }

      builder.delegateRandomToken(getRandomTokenForEcsDelegate());
      builder.sequenceNum(getSequenceNumForEcsDelegate());
    } catch (Exception e) {
      log.warn("Failed while reading seqNum and delegateToken from disk file", e);
    }
  }

  private String getSequenceConfigData() {
    if (!isEcsDelegate()) {
      return "";
    }

    try {
      FileUtils.touch(new File(DELEGATE_SEQUENCE_CONFIG_FILE));
      return FileIo.getFileContentsWithSharedLockAcrossProcesses(DELEGATE_SEQUENCE_CONFIG_FILE);
    } catch (Exception e) {
      return "";
    }
  }

  private String getSequenceNumForEcsDelegate() {
    if (!isEcsDelegate()) {
      return null;
    }
    String content = getSequenceConfigData();
    if (isBlank(content)) {
      return null;
    }

    String seqNum = content.substring(content.lastIndexOf(SEQ) + 5);
    return isBlank(seqNum) ? null : seqNum;
  }

  private String getRandomTokenForEcsDelegate() {
    if (!isEcsDelegate()) {
      return null;
    }

    String token = readTokenFromFile();

    if (token == null) {
      FileUtils.deleteQuietly(new File(DELEGATE_SEQUENCE_CONFIG_FILE));
      generateEcsDelegateSequenceConfigFile();
      token = readTokenFromFile();
    }

    return token;
  }

  private String readTokenFromFile() {
    if (!isEcsDelegate()) {
      return null;
    }
    String content = getSequenceConfigData();
    if (isBlank(content)) {
      return null;
    }

    String token = content.substring(7, content.lastIndexOf(SEQ));
    return isBlank(token) ? null : token;
  }

  private void handleEcsDelegateRegistrationResponse(DelegateRegisterResponse delegateResponse) {
    if (!isEcsDelegate()) {
      return;
    }

    try {
      FileIo.writeWithExclusiveLockAcrossProcesses(
          TOKEN + delegateResponse.getDelegateRandomToken() + SEQ + delegateResponse.getSequenceNum(),
          DELEGATE_SEQUENCE_CONFIG_FILE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (Exception e) {
      log.error("Failed to write registration response into delegate_sequence file", e);
    }
  }

  private boolean isEcsDelegate() {
    return "ECS".equals(DELEGATE_TYPE);
  }

  private void generateEcsDelegateSequenceConfigFile() {
    try {
      FileUtils.touch(new File(DELEGATE_SEQUENCE_CONFIG_FILE));
      String randomToken = UUIDGenerator.generateUuid();
      FileIo.writeWithExclusiveLockAcrossProcesses(
          TOKEN + randomToken + SEQ, DELEGATE_SEQUENCE_CONFIG_FILE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      log.warn("Failed to create DelegateSequenceConfigFile", e);
    }
  }

  private void addToEncryptedConfigListMap(Map<EncryptionConfig, List<EncryptedRecord>> encryptionConfigListMap,
      EncryptionConfig encryptionConfig, EncryptedRecord encryptedRecord) {
    if (encryptionConfigListMap.containsKey(encryptionConfig)) {
      encryptionConfigListMap.get(encryptionConfig).add(encryptedRecord);
    } else {
      List<EncryptedRecord> encryptedRecordList = new ArrayList<>();
      encryptedRecordList.add(encryptedRecord);
      encryptionConfigListMap.put(encryptionConfig, encryptedRecordList);
    }
  }

  @VisibleForTesting
  void applyDelegateSecretFunctor(DelegateTaskPackage delegateTaskPackage) {
    Map<String, EncryptionConfig> encryptionConfigs = delegateTaskPackage.getEncryptionConfigs();
    Map<String, SecretDetail> secretDetails = delegateTaskPackage.getSecretDetails();
    if (isEmpty(encryptionConfigs) || isEmpty(secretDetails)) {
      return;
    }
    List<EncryptedRecord> encryptedRecordList = new ArrayList<>();
    Map<EncryptionConfig, List<EncryptedRecord>> encryptionConfigListMap = new HashMap<>();
    secretDetails.forEach((key, secretDetail) -> {
      encryptedRecordList.add(secretDetail.getEncryptedRecord());
      // encryptionConfigListMap.put(encryptionConfigs.get(secretDetail.getConfigUuid()), encryptedRecordList);
      addToEncryptedConfigListMap(encryptionConfigListMap, encryptionConfigs.get(secretDetail.getConfigUuid()),
          secretDetail.getEncryptedRecord());
    });

    Map<String, char[]> decryptedRecords = delegateDecryptionService.decrypt(encryptionConfigListMap);
    Map<String, char[]> secretUuidToValues = new HashMap<>();

    secretDetails.forEach((key, value) -> {
      char[] secretValue = decryptedRecords.get(value.getEncryptedRecord().getUuid());
      secretUuidToValues.put(key, secretValue);

      // Adds secret values from the 3 phase decryption to the list of task secrets to be masked
      delegateTaskPackage.getSecrets().add(String.valueOf(secretValue));
    });

    DelegateExpressionEvaluator delegateExpressionEvaluator =
        new DelegateExpressionEvaluator(secretUuidToValues, delegateTaskPackage.getData().getExpressionFunctorToken());
    TaskData taskData = delegateTaskPackage.getData();
    if (taskData.getParameters() != null && taskData.getParameters().length == 1
        && taskData.getParameters()[0] instanceof TaskParameters) {
      log.info("Applying DelegateExpression Evaluator for delegateTask");
      ExpressionReflectionUtils.applyExpression(taskData.getParameters()[0],
          (secretMode, value) -> delegateExpressionEvaluator.substitute(value, new HashMap<>()));
    }
  }

  private boolean shouldContactManager() {
    return !selfDestruct.get();
  }
}
