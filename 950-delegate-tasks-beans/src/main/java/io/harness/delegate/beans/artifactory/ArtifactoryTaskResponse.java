package io.harness.delegate.beans.artifactory;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorValidationResult;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArtifactoryTaskResponse implements DelegateTaskNotifyResponseData {
  ConnectorValidationResult connectorValidationResult;
  DelegateMetaInfo delegateMetaInfo;
}
