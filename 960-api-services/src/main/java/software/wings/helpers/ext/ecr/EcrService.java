package software.wings.helpers.ext.ecr;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.ecr.beans.AwsInternalConfig;

import java.util.List;
import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.CDC;

/**
 * Created by brett on 7/15/17
 */
@OwnedBy(CDC)
public interface EcrService {
  int MAX_NO_OF_TAGS_PER_IMAGE = 10000;
  /**
   * Gets builds.
   *
   * @param awsConfig         the aws cloud provider config
   * @param region            the region name
   * @param imageName         the image name
   * @param maxNumberOfBuilds the max number of builds
   * @return the builds
   */
  List<BuildDetailsInternal> getBuilds(
      AwsInternalConfig awsConfig, String imageUrl, String region, String imageName, int maxNumberOfBuilds);

  /**
   * Gets last successful build.
   *
   * @param awsConfig the ecr config
   * @param imageName the image name
   * @return the last successful build
   */
  BuildDetailsInternal getLastSuccessfulBuild(AwsInternalConfig awsConfig, String imageName);

  /**
   * Validates the Image
   *
   * @param awsConfig the ecr config
   * @param region    the aws region
   * @param imageName the image name
   * @return the boolean
   */
  boolean verifyRepository(AwsInternalConfig awsConfig, String region, String imageName);

  /**
   * Lists aws regions
   *
   * @param awsConfig aws config
   * @return
   */
  List<String> listRegions(AwsInternalConfig awsConfig);

  /**
   * List ecr registry list.
   *
   * @param awsConfig the ecr config
   * @return the list
   */
  List<String> listEcrRegistry(AwsInternalConfig awsConfig, String region);

  List<Map<String, String>> getLabels(AwsInternalConfig awsConfig, String imageName, String region, List<String> tags);

  boolean validateCredentials(AwsInternalConfig awsConfig, String imageName);

  BuildDetailsInternal verifyBuildNumber(AwsInternalConfig awsInternalConfig, String imageUrl, String region, String imageName, String tag);

  BuildDetailsInternal getLastSuccessfulBuildFromRegex(AwsInternalConfig awsInternalConfig, String imageUrl, String region, String imageName, String tagRegex);
}
