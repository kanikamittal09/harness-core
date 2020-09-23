package io.harness.cdng.pipeline.executions.beans;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ExecutionNodeAdjacencyList {
  List<String> children;
  String next;
}
