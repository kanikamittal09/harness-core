package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.cvnglog.ExecutionLogDTO;


public interface ExecutionLogger {
  default void info(String message) {
    this.log(ExecutionLogDTO.LogLevel.INFO, message);
  }
  default void warn(String message) {
    this.log(ExecutionLogDTO.LogLevel.INFO, message);
  }
  default void error(String message) {
    this.log(ExecutionLogDTO.LogLevel.INFO, message);
  }


  void log(ExecutionLogDTO.LogLevel logLevel, String message);
}
