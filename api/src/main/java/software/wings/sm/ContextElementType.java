package software.wings.sm;

/**
 * Describes what type of element is being repeated on.
 *
 * @author Rishi
 */
public enum ContextElementType {
  /**
   * Service context element type.
   */
  SERVICE,

  /**
   * Service context element type.
   */
  SERVICE_TEMPLATE,

  /**
   * Tag context element type.
   */
  TAG,

  /**
   * Host context element type.
   */
  HOST,

  /**
   * Instance context element type.
   */
  INSTANCE,

  /**
   * Standard context element type.
   */
  STANDARD,

  /**
   * Param context element type.
   */
  PARAM,

  /**
   * Partition context element type.
   */
  PARTITION,

  /**
   * Other context element type.
   */
  OTHER,

  /**
   * Fork context element type.
   */
  FORK,

  /**
   * Container cluster - ECS/Kubernetes context element type.
   */
  CONTAINER_SERVICE,

  /**
   * Cluster context element type.
   */
  CLUSTER,

  /**
   * Aws lambda function context element type.
   */
  AWS_LAMBDA_FUNCTION, /**
                        * Ami service context element type.
                        */
  AMI_SERVICE_SETUP, /**
                      * Ami service deploy context element type.
                      */
  AMI_SERVICE_DEPLOY, /**
                       * Artifact context element type.
                       */
  ARTIFACT, /**
             * Helm deploy context element type.
             */
  HELM_DEPLOY
}
