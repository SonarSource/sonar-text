/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.common.git.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ProcessBuilderWrapper {
  private static final Logger LOG = LoggerFactory.getLogger(ProcessBuilderWrapper.class);
  private static final long TIMEOUT_MILLIS = 30_000;
  private static final ExecutorService PROCESS_MONITOR = Executors.newSingleThreadExecutor();

  public Status execute(List<String> command, Consumer<String> lineConsumer) throws IOException {
    var process = startProcess(command);
    var readerFuture = PROCESS_MONITOR.submit(() -> readProcessOutput(process, lineConsumer));

    try {
      var exited = process.waitFor(TIMEOUT_MILLIS, MILLISECONDS);
      if (!exited) {
        LOG.debug("Process {} did not exit within {} ms", command, TIMEOUT_MILLIS);
        return Status.FAILURE;
      }

      if (process.exitValue() != 0) {
        LOG.debug("Process {} exited with code {}", command, process.exitValue());
        return Status.FAILURE;
      }
      readerFuture.get();
      return Status.SUCCESS;
    } catch (InterruptedException | ExecutionException e) {
      LOG.debug("Error while executing process {}", command, e);
      return Status.FAILURE;
    } finally {
      process.destroy();
    }
  }

  Process startProcess(List<String> command) throws IOException {
    var processBuilder = new ProcessBuilder(command);
    return processBuilder.start();
  }

  private static void readProcessOutput(Process process, Consumer<String> lineConsumer) {
    try (var scanner = new Scanner(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      while (scanner.hasNextLine()) {
        lineConsumer.accept(scanner.nextLine());
      }
    }
  }

  public enum Status {
    SUCCESS,
    FAILURE
  }
}
