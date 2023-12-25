// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: admingrpc.proto

package com.alipay.antchain.bridge.relayer.core.grpc.admin;

public interface AdminRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:acb.relayer.admin.AdminRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * commandNamespace
   * </pre>
   *
   * <code>string commandNamespace = 1;</code>
   * @return The commandNamespace.
   */
  java.lang.String getCommandNamespace();
  /**
   * <pre>
   * commandNamespace
   * </pre>
   *
   * <code>string commandNamespace = 1;</code>
   * @return The bytes for commandNamespace.
   */
  com.google.protobuf.ByteString
      getCommandNamespaceBytes();

  /**
   * <pre>
   * 管理命令
   * </pre>
   *
   * <code>string command = 2;</code>
   * @return The command.
   */
  java.lang.String getCommand();
  /**
   * <pre>
   * 管理命令
   * </pre>
   *
   * <code>string command = 2;</code>
   * @return The bytes for command.
   */
  com.google.protobuf.ByteString
      getCommandBytes();

  /**
   * <pre>
   * 参数
   * </pre>
   *
   * <code>repeated string args = 3;</code>
   * @return A list containing the args.
   */
  java.util.List<java.lang.String>
      getArgsList();
  /**
   * <pre>
   * 参数
   * </pre>
   *
   * <code>repeated string args = 3;</code>
   * @return The count of args.
   */
  int getArgsCount();
  /**
   * <pre>
   * 参数
   * </pre>
   *
   * <code>repeated string args = 3;</code>
   * @param index The index of the element to return.
   * @return The args at the given index.
   */
  java.lang.String getArgs(int index);
  /**
   * <pre>
   * 参数
   * </pre>
   *
   * <code>repeated string args = 3;</code>
   * @param index The index of the value to return.
   * @return The bytes of the args at the given index.
   */
  com.google.protobuf.ByteString
      getArgsBytes(int index);
}