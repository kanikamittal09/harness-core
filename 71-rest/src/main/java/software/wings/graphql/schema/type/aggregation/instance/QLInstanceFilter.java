package software.wings.graphql.schema.type.aggregation.instance;

import lombok.Builder;
import lombok.Data;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.instance.QLInstanceType;

@Data
@Builder
public class QLInstanceFilter implements EntityFilter {
  private QLTimeFilter createdAt;
  private QLIdFilter application;
  private QLIdFilter service;
  private QLIdFilter environment;
  private QLIdFilter cloudProvider;
  private QLInstanceType instanceType;
  private QLInstanceTagFilter tag;
}
