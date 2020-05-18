// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/perpetual_task_service.proto

package io.harness.perpetualtask;

/**
 * Protobuf type {@code io.harness.perpetualtask.PerpetualTaskListRequest}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:PerpetualTaskListRequest.java.pb.meta")
public final class PerpetualTaskListRequest extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.perpetualtask.PerpetualTaskListRequest)
    PerpetualTaskListRequestOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use PerpetualTaskListRequest.newBuilder() to construct.
  private PerpetualTaskListRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private PerpetualTaskListRequest() {}

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private PerpetualTaskListRequest(
      com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
    com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            io.harness.delegate.DelegateId.Builder subBuilder = null;
            if (delegateId_ != null) {
              subBuilder = delegateId_.toBuilder();
            }
            delegateId_ = input.readMessage(io.harness.delegate.DelegateId.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(delegateId_);
              delegateId_ = subBuilder.buildPartial();
            }

            break;
          }
          default: {
            if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return io.harness.perpetualtask.PerpetualTaskServiceOuterClass
        .internal_static_io_harness_perpetualtask_PerpetualTaskListRequest_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.perpetualtask.PerpetualTaskServiceOuterClass
        .internal_static_io_harness_perpetualtask_PerpetualTaskListRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(io.harness.perpetualtask.PerpetualTaskListRequest.class,
            io.harness.perpetualtask.PerpetualTaskListRequest.Builder.class);
  }

  public static final int DELEGATE_ID_FIELD_NUMBER = 1;
  private io.harness.delegate.DelegateId delegateId_;
  /**
   * <code>.io.harness.delegate.DelegateId delegate_id = 1;</code>
   */
  public boolean hasDelegateId() {
    return delegateId_ != null;
  }
  /**
   * <code>.io.harness.delegate.DelegateId delegate_id = 1;</code>
   */
  public io.harness.delegate.DelegateId getDelegateId() {
    return delegateId_ == null ? io.harness.delegate.DelegateId.getDefaultInstance() : delegateId_;
  }
  /**
   * <code>.io.harness.delegate.DelegateId delegate_id = 1;</code>
   */
  public io.harness.delegate.DelegateIdOrBuilder getDelegateIdOrBuilder() {
    return getDelegateId();
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1)
      return true;
    if (isInitialized == 0)
      return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
    if (delegateId_ != null) {
      output.writeMessage(1, getDelegateId());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (delegateId_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, getDelegateId());
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof io.harness.perpetualtask.PerpetualTaskListRequest)) {
      return super.equals(obj);
    }
    io.harness.perpetualtask.PerpetualTaskListRequest other = (io.harness.perpetualtask.PerpetualTaskListRequest) obj;

    if (hasDelegateId() != other.hasDelegateId())
      return false;
    if (hasDelegateId()) {
      if (!getDelegateId().equals(other.getDelegateId()))
        return false;
    }
    if (!unknownFields.equals(other.unknownFields))
      return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    if (hasDelegateId()) {
      hash = (37 * hash) + DELEGATE_ID_FIELD_NUMBER;
      hash = (53 * hash) + getDelegateId().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.perpetualtask.PerpetualTaskListRequest parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.PerpetualTaskListRequest parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.PerpetualTaskListRequest parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.PerpetualTaskListRequest parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.PerpetualTaskListRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.PerpetualTaskListRequest parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.PerpetualTaskListRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.PerpetualTaskListRequest parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.PerpetualTaskListRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.PerpetualTaskListRequest parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.PerpetualTaskListRequest parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.PerpetualTaskListRequest parseFrom(com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() {
    return newBuilder();
  }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(io.harness.perpetualtask.PerpetualTaskListRequest prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code io.harness.perpetualtask.PerpetualTaskListRequest}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.perpetualtask.PerpetualTaskListRequest)
      io.harness.perpetualtask.PerpetualTaskListRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.perpetualtask.PerpetualTaskServiceOuterClass
          .internal_static_io_harness_perpetualtask_PerpetualTaskListRequest_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.perpetualtask.PerpetualTaskServiceOuterClass
          .internal_static_io_harness_perpetualtask_PerpetualTaskListRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(io.harness.perpetualtask.PerpetualTaskListRequest.class,
              io.harness.perpetualtask.PerpetualTaskListRequest.Builder.class);
    }

    // Construct using io.harness.perpetualtask.PerpetualTaskListRequest.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      if (delegateIdBuilder_ == null) {
        delegateId_ = null;
      } else {
        delegateId_ = null;
        delegateIdBuilder_ = null;
      }
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.perpetualtask.PerpetualTaskServiceOuterClass
          .internal_static_io_harness_perpetualtask_PerpetualTaskListRequest_descriptor;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.PerpetualTaskListRequest getDefaultInstanceForType() {
      return io.harness.perpetualtask.PerpetualTaskListRequest.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.PerpetualTaskListRequest build() {
      io.harness.perpetualtask.PerpetualTaskListRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.PerpetualTaskListRequest buildPartial() {
      io.harness.perpetualtask.PerpetualTaskListRequest result =
          new io.harness.perpetualtask.PerpetualTaskListRequest(this);
      if (delegateIdBuilder_ == null) {
        result.delegateId_ = delegateId_;
      } else {
        result.delegateId_ = delegateIdBuilder_.build();
      }
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field, int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof io.harness.perpetualtask.PerpetualTaskListRequest) {
        return mergeFrom((io.harness.perpetualtask.PerpetualTaskListRequest) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.perpetualtask.PerpetualTaskListRequest other) {
      if (other == io.harness.perpetualtask.PerpetualTaskListRequest.getDefaultInstance())
        return this;
      if (other.hasDelegateId()) {
        mergeDelegateId(other.getDelegateId());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
      io.harness.perpetualtask.PerpetualTaskListRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.perpetualtask.PerpetualTaskListRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private io.harness.delegate.DelegateId delegateId_;
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.delegate.DelegateId,
        io.harness.delegate.DelegateId.Builder, io.harness.delegate.DelegateIdOrBuilder> delegateIdBuilder_;
    /**
     * <code>.io.harness.delegate.DelegateId delegate_id = 1;</code>
     */
    public boolean hasDelegateId() {
      return delegateIdBuilder_ != null || delegateId_ != null;
    }
    /**
     * <code>.io.harness.delegate.DelegateId delegate_id = 1;</code>
     */
    public io.harness.delegate.DelegateId getDelegateId() {
      if (delegateIdBuilder_ == null) {
        return delegateId_ == null ? io.harness.delegate.DelegateId.getDefaultInstance() : delegateId_;
      } else {
        return delegateIdBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.harness.delegate.DelegateId delegate_id = 1;</code>
     */
    public Builder setDelegateId(io.harness.delegate.DelegateId value) {
      if (delegateIdBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        delegateId_ = value;
        onChanged();
      } else {
        delegateIdBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.delegate.DelegateId delegate_id = 1;</code>
     */
    public Builder setDelegateId(io.harness.delegate.DelegateId.Builder builderForValue) {
      if (delegateIdBuilder_ == null) {
        delegateId_ = builderForValue.build();
        onChanged();
      } else {
        delegateIdBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.harness.delegate.DelegateId delegate_id = 1;</code>
     */
    public Builder mergeDelegateId(io.harness.delegate.DelegateId value) {
      if (delegateIdBuilder_ == null) {
        if (delegateId_ != null) {
          delegateId_ = io.harness.delegate.DelegateId.newBuilder(delegateId_).mergeFrom(value).buildPartial();
        } else {
          delegateId_ = value;
        }
        onChanged();
      } else {
        delegateIdBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.delegate.DelegateId delegate_id = 1;</code>
     */
    public Builder clearDelegateId() {
      if (delegateIdBuilder_ == null) {
        delegateId_ = null;
        onChanged();
      } else {
        delegateId_ = null;
        delegateIdBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.harness.delegate.DelegateId delegate_id = 1;</code>
     */
    public io.harness.delegate.DelegateId.Builder getDelegateIdBuilder() {
      onChanged();
      return getDelegateIdFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.harness.delegate.DelegateId delegate_id = 1;</code>
     */
    public io.harness.delegate.DelegateIdOrBuilder getDelegateIdOrBuilder() {
      if (delegateIdBuilder_ != null) {
        return delegateIdBuilder_.getMessageOrBuilder();
      } else {
        return delegateId_ == null ? io.harness.delegate.DelegateId.getDefaultInstance() : delegateId_;
      }
    }
    /**
     * <code>.io.harness.delegate.DelegateId delegate_id = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.delegate.DelegateId,
        io.harness.delegate.DelegateId.Builder, io.harness.delegate.DelegateIdOrBuilder>
    getDelegateIdFieldBuilder() {
      if (delegateIdBuilder_ == null) {
        delegateIdBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<io.harness.delegate.DelegateId,
            io.harness.delegate.DelegateId.Builder, io.harness.delegate.DelegateIdOrBuilder>(
            getDelegateId(), getParentForChildren(), isClean());
        delegateId_ = null;
      }
      return delegateIdBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.perpetualtask.PerpetualTaskListRequest)
  }

  // @@protoc_insertion_point(class_scope:io.harness.perpetualtask.PerpetualTaskListRequest)
  private static final io.harness.perpetualtask.PerpetualTaskListRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.perpetualtask.PerpetualTaskListRequest();
  }

  public static io.harness.perpetualtask.PerpetualTaskListRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<PerpetualTaskListRequest> PARSER =
      new com.google.protobuf.AbstractParser<PerpetualTaskListRequest>() {
        @java.lang.Override
        public PerpetualTaskListRequest parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new PerpetualTaskListRequest(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<PerpetualTaskListRequest> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<PerpetualTaskListRequest> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.perpetualtask.PerpetualTaskListRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
