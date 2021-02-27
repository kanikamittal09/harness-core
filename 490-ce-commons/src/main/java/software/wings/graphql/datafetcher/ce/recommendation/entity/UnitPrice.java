package software.wings.graphql.datafetcher.ce.recommendation.entity;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UnitPrice {
  BigDecimal cpu;
  BigDecimal memory;
}
