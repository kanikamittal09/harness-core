package io.harness.pms.execution;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.queue.Queuable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "SdkResponseEventKeys")
@Entity(value = "SdkResponseEvent", noClassnameStored = true)
@Document("SdkResponseEvent")
@HarnessEntity(exportable = false)
@TypeAlias("SdkResponseEvent")
public class SdkResponseEvent extends Queuable {
  @Singular List<SdkResponseEventInternal> sdkResponseEventInternals;

  public AutoLogContext autoLogContext() {
    Map<String, String> logContext = new HashMap<>();
    return new AutoLogContext(logContext, OVERRIDE_NESTS);
  }
}
