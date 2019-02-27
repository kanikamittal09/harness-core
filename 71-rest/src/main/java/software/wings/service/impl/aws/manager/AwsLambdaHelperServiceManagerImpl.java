package software.wings.service.impl.aws.manager;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.waiter.ErrorNotifyResponseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsLambdaFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaFunctionResponse;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.aws.manager.AwsLambdaHelperServiceManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Pranjal on 01/29/2019
 */
public class AwsLambdaHelperServiceManagerImpl implements AwsLambdaHelperServiceManager {
  private static final Logger logger = LoggerFactory.getLogger(AwsLambdaHelperServiceManagerImpl.class);
  private static final long TIME_OUT_IN_MINUTES = 2;
  @Inject private DelegateService delegateService;

  @Override
  public List<String> listLambdaFunctions(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    AwsResponse response = executeTask(awsConfig.getAccountId(),
        AwsLambdaFunctionRequest.builder()
            .awsConfig(awsConfig)
            .encryptionDetails(encryptionDetails)
            .region(region)
            .build());
    logger.info("Listing Lambda functions for region {}", region);
    return ((AwsLambdaFunctionResponse) response).getLambdaFunctions();
  }

  private AwsResponse executeTask(String accountId, AwsLambdaFunctionRequest request) {
    DelegateTask delegateTask =
        aDelegateTask()
            .withTaskType(TaskType.AWS_LAMBDA_TASK.name())
            .withAccountId(accountId)
            .withAsync(false)
            .withTags(
                isNotEmpty(request.getAwsConfig().getTag()) ? singletonList(request.getAwsConfig().getTag()) : null)
            .withTimeout(TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES))
            .withParameters(new Object[] {request})
            .build();
    try {
      ResponseData notifyResponseData = delegateService.executeTask(delegateTask);
      if (notifyResponseData instanceof ErrorNotifyResponseData
          || ((AwsLambdaFunctionResponse) notifyResponseData).getExecutionStatus().equals(ExecutionStatus.FAILED)) {
        throw new WingsException(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
      }
      return (AwsResponse) notifyResponseData;
    } catch (InterruptedException ex) {
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }
  }
}
