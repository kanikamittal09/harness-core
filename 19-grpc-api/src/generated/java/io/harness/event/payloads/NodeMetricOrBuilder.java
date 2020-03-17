// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/event/payloads/k8s_utilization_messages.proto

package io.harness.event.payloads;

@javax.annotation.Generated(value = "protoc", comments = "annotations:NodeMetricOrBuilder.java.pb.meta")
public interface NodeMetricOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.event.payloads.NodeMetric)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>string cloud_provider_id = 1;</code>
   */
  java.lang.String getCloudProviderId();
  /**
   * <code>string cloud_provider_id = 1;</code>
   */
  com.google.protobuf.ByteString getCloudProviderIdBytes();

  /**
   * <code>string name = 2;</code>
   */
  java.lang.String getName();
  /**
   * <code>string name = 2;</code>
   */
  com.google.protobuf.ByteString getNameBytes();

  /**
   * <code>.google.protobuf.Timestamp timestamp = 3;</code>
   */
  boolean hasTimestamp();
  /**
   * <code>.google.protobuf.Timestamp timestamp = 3;</code>
   */
  com.google.protobuf.Timestamp getTimestamp();
  /**
   * <code>.google.protobuf.Timestamp timestamp = 3;</code>
   */
  com.google.protobuf.TimestampOrBuilder getTimestampOrBuilder();

  /**
   * <code>.google.protobuf.Duration window = 4;</code>
   */
  boolean hasWindow();
  /**
   * <code>.google.protobuf.Duration window = 4;</code>
   */
  com.google.protobuf.Duration getWindow();
  /**
   * <code>.google.protobuf.Duration window = 4;</code>
   */
  com.google.protobuf.DurationOrBuilder getWindowOrBuilder();

  /**
   * <code>.io.harness.event.payloads.Usage usage = 5;</code>
   */
  boolean hasUsage();
  /**
   * <code>.io.harness.event.payloads.Usage usage = 5;</code>
   */
  io.harness.event.payloads.Usage getUsage();
  /**
   * <code>.io.harness.event.payloads.Usage usage = 5;</code>
   */
  io.harness.event.payloads.UsageOrBuilder getUsageOrBuilder();

  /**
   * <code>string cluster_id = 6;</code>
   */
  java.lang.String getClusterId();
  /**
   * <code>string cluster_id = 6;</code>
   */
  com.google.protobuf.ByteString getClusterIdBytes();

  /**
   * <code>string kube_system_uid = 7;</code>
   */
  java.lang.String getKubeSystemUid();
  /**
   * <code>string kube_system_uid = 7;</code>
   */
  com.google.protobuf.ByteString getKubeSystemUidBytes();
}
