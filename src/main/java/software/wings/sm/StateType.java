package software.wings.sm;

import static org.joor.Reflect.on;
import static software.wings.sm.StateTypeScope.ORCHESTRATION_STENCILS;
import static software.wings.sm.StateTypeScope.PIPELINE_STENCILS;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.exception.WingsException;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.BuildState;
import software.wings.sm.states.EmailState;
import software.wings.sm.states.EnvState;
import software.wings.sm.states.ForkState;
import software.wings.sm.states.HttpState;
import software.wings.sm.states.PauseState;
import software.wings.sm.states.RepeatState;
import software.wings.sm.states.StartState;
import software.wings.sm.states.StopState;
import software.wings.sm.states.WaitState;
import software.wings.utils.JsonUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * Represents type of state.
 *
 * @author Rishi
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum StateType implements StateTypeDescriptor {
  /**
   * Repeat state type.
   */
  REPEAT(RepeatState.class, ORCHESTRATION_STENCILS),

  /**
   * Fork state type.
   */
  FORK(ForkState.class, ORCHESTRATION_STENCILS),

  // STATE_MACHINE(ORCHESTRATION_STENCILS),

  /**
   * Wait state type.
   */
  WAIT(WaitState.class, ORCHESTRATION_STENCILS),

  /**
   * Pause state type.
   */
  PAUSE(PauseState.class, ORCHESTRATION_STENCILS),

  /**
   * Start state type.
   */
  // DEPLOY(ORCHESTRATION_STENCILS),
  START(StartState.class, ORCHESTRATION_STENCILS),

  /**
   * Stop state type.
   */
  STOP(StopState.class, ORCHESTRATION_STENCILS),
  // RESTART(ORCHESTRATION_STENCILS),

  /**
   * Http state type.
   */
  HTTP(HttpState.class, ORCHESTRATION_STENCILS), // SPLUNK(ORCHESTRATION_STENCILS),
                                                 // APP_DYNAMICS(ORCHESTRATION_STENCILS),

  /**
   * Email state type.
   */
  EMAIL(EmailState.class, ORCHESTRATION_STENCILS),

  /**
   * Build state type.
   */
  BUILD(BuildState.class, PIPELINE_STENCILS),

  /**
   * Env state state type.
   */
  ENV_STATE(EnvState.class, PIPELINE_STENCILS),

  /**
   * Approval state type.
   */
  APPROVAL(ApprovalState.class, ORCHESTRATION_STENCILS, PIPELINE_STENCILS);

  private static final String stencilsPath = "/templates/stencils/";
  private static final String uiSchemaSuffix = "-UISchema.json";
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private Class<? extends State> stateClass;
  private Object jsonSchema;
  private Object uiSchema;
  private List<StateTypeScope> scopes = new ArrayList<>();

  /**
   * Instantiates a new state type.
   *
   * @param stateClass the state class
   * @param scopes     the scopes
   */
  StateType(Class<? extends State> stateClass, StateTypeScope... scopes) {
    this.stateClass = stateClass;
    this.scopes = Arrays.asList(scopes);
    this.jsonSchema = loadJsonSchema();
    this.uiSchema = readResource(stencilsPath + name() + uiSchemaSuffix);
  }

  private Object readResource(String file) {
    try {
      URL url = this.getClass().getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception exception) {
      WingsException ex = new WingsException("Error in initializing StateType-" + file, exception);
      logger.error(ex.getMessage(), ex);
      return null;
      // throw ex;
      // TODO - uncomment exception later on
    }
  }

  @Override
  public String getType() {
    return name();
  }

  @Override
  public Class<? extends State> getStateClass() {
    return stateClass;
  }

  @Override
  public Object getJsonSchema() {
    return ((JsonNode) jsonSchema).deepCopy();
  }

  @Override
  public Object getUiSchema() {
    return uiSchema;
  }

  @Override
  public List<StateTypeScope> getScopes() {
    return scopes;
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.sm.StateTypeDescriptor#newInstance(java.lang.String)
   */
  @Override
  public State newInstance(String id) {
    return on(stateClass).create(id).get();
  }

  private JsonNode loadJsonSchema() {
    return JsonUtils.jsonSchema(stateClass);
  }
}
