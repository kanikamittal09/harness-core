package software.wings.beans.infrastructure;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import software.wings.beans.NameValuePair;

import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@Entity(value = "cloudFormationRollbackConfig")
@HarnessEntity(exportable = true)
@FieldNameConstants(innerTypeName = "CloudFormationRollbackConfigKeys")
@OwnedBy(CDP)
@StoreIn(DbAliases.CG_MANAGER)
public class CloudFormationRollbackConfig implements PersistentEntity, UuidAware, CreatedAtAware, AccountAccess {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @FdIndex private String accountId;
  @FdIndex @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore @FdIndex private long createdAt;

  private String url;
  private String body;
  private String createType;
  private List<NameValuePair> variables;

  private String region;
  private String awsConfigId;
  private String customStackName;
  private String workflowExecutionId;
  private String cloudFormationRoleArn;
  @FdIndex private String entityId;
}
