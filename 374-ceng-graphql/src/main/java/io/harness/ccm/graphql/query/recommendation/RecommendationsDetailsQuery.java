package io.harness.ccm.graphql.query.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static com.google.common.base.MoreObjects.firstNonNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.ResourceType;
import io.harness.ccm.commons.beans.recommendation.models.RecommendClusterRequestDTO;
import io.harness.ccm.graphql.core.recommendation.NodeRecommendationService;
import io.harness.ccm.graphql.core.recommendation.WorkloadRecommendationService;
import io.harness.ccm.graphql.dto.recommendation.RecommendationDetailsDTO;
import io.harness.ccm.graphql.dto.recommendation.RecommendationItemDTO;
import io.harness.ccm.graphql.utils.GraphQLUtils;
import io.harness.ccm.graphql.utils.annotations.GraphQLApi;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.time.OffsetDateTime;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@GraphQLApi
@OwnedBy(CE)
public class RecommendationsDetailsQuery {
  @Inject private GraphQLUtils graphQLUtils;
  @Inject private WorkloadRecommendationService workloadRecommendationService;
  @Inject private NodeRecommendationService nodeRecommendationService;

  /**
   * Note: If this query becomes slow due to n+1 serial calls in future. Then,
   * we can use {@code CompletableFuture<RecommendationDetailsDTO>} to parallelize it,
   * with optional dataLoader to make 1+1 db calls instead of n+1.
   */
  @GraphQLQuery(description = "Recommendation details")
  public RecommendationDetailsDTO recommendationDetails(@GraphQLContext RecommendationItemDTO nodeDTO,
      @GraphQLArgument(name = "startTime", description = "defaults to Now().minusDays(7)") OffsetDateTime startTime,
      @GraphQLArgument(name = "endTime", description = "defaults to Now()") OffsetDateTime endTime,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountIdentifier = graphQLUtils.getAccountIdentifier(env);

    return recommendationDetailsInternal(
        accountIdentifier, nodeDTO.getResourceType(), nodeDTO.getId(), startTime, endTime);
  }

  @GraphQLQuery(description = "Recommendation details")
  public RecommendationDetailsDTO recommendationDetails(
      @GraphQLNonNull @GraphQLArgument(name = "id", description = "The identifier of the Recommendation") String id,
      @GraphQLNonNull @GraphQLArgument(
          name = "resourceType", description = "Recommendation type") ResourceType resourceType,
      @GraphQLArgument(name = "startTime", description = "defaults to Now().minusDays(7)") OffsetDateTime startTime,
      @GraphQLArgument(name = "endTime", description = "defaults to Now()") OffsetDateTime endTime,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountIdentifier = graphQLUtils.getAccountIdentifier(env);

    return recommendationDetailsInternal(accountIdentifier, resourceType, id, startTime, endTime);
  }

  @GraphQLQuery(name = "nodeRecommendationRequest")
  public RecommendClusterRequestDTO nodeRecommendationRequest(
      @GraphQLNonNull @GraphQLArgument(name = "nodePoolId") NodePoolId nodePoolId,
      @GraphQLArgument(name = "startTime", description = "defaults to Now().minusDays(7)") OffsetDateTime startTime,
      @GraphQLArgument(name = "endTime", description = "defaults to Now()") OffsetDateTime endTime,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountIdentifier = graphQLUtils.getAccountIdentifier(env);

    return nodeRecommendationService.constructRecommendationRequest(accountIdentifier, nodePoolId,
        firstNonNull(startTime, OffsetDateTime.now().minusDays(7)), firstNonNull(endTime, OffsetDateTime.now()));
  }

  private RecommendationDetailsDTO recommendationDetailsInternal(@NotNull final String accountIdentifier,
      @NotNull ResourceType resourceType, @NotNull String id, @Nullable OffsetDateTime startTime,
      @Nullable OffsetDateTime endTime) {
    switch (resourceType) {
      case WORKLOAD:
        return workloadRecommendationService.getWorkloadRecommendationById(accountIdentifier, id,
            firstNonNull(startTime, OffsetDateTime.now().minusDays(7)), firstNonNull(endTime, OffsetDateTime.now()));
      case NODE_POOL:
        return nodeRecommendationService.getRecommendation(accountIdentifier, id);
      default:
        throw new InvalidRequestException(String.format("Recommendation not yet implemented for %s", resourceType));
    }
  }
}
