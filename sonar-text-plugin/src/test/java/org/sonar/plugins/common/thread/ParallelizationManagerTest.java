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
package org.sonar.plugins.common.thread;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ParallelizationManagerTest {
  private static final Logger LOG = LoggerFactory.getLogger(ParallelizationManagerTest.class);

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private ParallelizationManager executor = new ParallelizationManager(1);

  private static final Runnable RUNNABLE_DOING_NOTHING = () -> {
  };
  private static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException("msg");
  private static final Runnable RUNNABLE_WITH_RUNTIME_EXCEPTION = () -> {
    throw RUNTIME_EXCEPTION;
  };

  private static final IllegalStateException ILLEGAL_STATE_EXCEPTION = new IllegalStateException("msg");
  private static final Runnable RUNNABLE_WITH_ILLEGAL_STATE_EXCEPTION = () -> {
    throw ILLEGAL_STATE_EXCEPTION;
  };

  @BeforeEach
  void init() {
    executor = new ParallelizationManager(1);
  }

  @AfterEach
  void cleanUp() {
    executor.shutdown();
  }

  @Test
  void shouldRunTasksSuccessFully() {
    String expectedLogMessage = "Doing something";
    Runnable runnableDoingSomething = () -> LOG.debug(expectedLogMessage);

    executor.submit(runnableDoingSomething);
    executor.submit(runnableDoingSomething);
    executor.submit(runnableDoingSomething);

    // wait for all threads to finish
    executor.drainThreads();

    List<String> logs = logTester.logs(Level.DEBUG);
    assertThat(logs)
      .hasSize(3)
      .containsOnly(expectedLogMessage);
  }

  @Test
  void shouldPropagateExceptionWhenSubmittingNewTaskAfterRunnableCrashedPreviously() {

    executor.submit(RUNNABLE_DOING_NOTHING);
    executor.submit(RUNNABLE_WITH_RUNTIME_EXCEPTION);

    // as the previous task should exit with an exception, this task should propagate the exception
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> executor.submit(RUNNABLE_DOING_NOTHING))
      .withCause(RUNTIME_EXCEPTION);
  }

  @Test
  void shouldDrainSuccessfully() {
    executor.submit(RUNNABLE_DOING_NOTHING);
    assertThatNoException().isThrownBy(() -> executor.drainThreads());
  }

  @Test
  void shouldPropagateExceptionWhenTerminatingThreadPoolAfterRunnableCrashedPreviously() {
    executor.submit(RUNNABLE_WITH_RUNTIME_EXCEPTION);

    // as the previous task should exit with an exception, this task should propagate the exception
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(executor::shutdown).withCause(RUNTIME_EXCEPTION);
  }

  @Test
  void shouldRejectRunnableWhenAlreadyShutdown() {
    executor.shutdown();

    // as the previous task should exit with an exception, this task should propagate the exception
    assertThatExceptionOfType(RejectedExecutionException.class).isThrownBy(
      () -> executor.submit(RUNNABLE_DOING_NOTHING));
  }

  @Test
  void shouldPropagateIllegalStateException() {
    executor.submit(RUNNABLE_DOING_NOTHING);
    executor.submit(RUNNABLE_WITH_ILLEGAL_STATE_EXCEPTION);

    // as the previous runnable should crash with an exception, this task should propagate the exception
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> executor.submit(RUNNABLE_DOING_NOTHING))
      .withMessage(ILLEGAL_STATE_EXCEPTION.getMessage());
  }

  @Test
  void shouldPropagateIllegalStateExceptionWhenDraining() {
    executor.submit(RUNNABLE_DOING_NOTHING);
    executor.submit(RUNNABLE_WITH_ILLEGAL_STATE_EXCEPTION);

    // as the previous runnable should crash with an exception, this task should propagate the exception
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(executor::drainThreads)
      .withMessage(ILLEGAL_STATE_EXCEPTION.getMessage());
  }

  @Test
  void shouldPropagateIllegalStateExceptionWhenTerminatingThreadPool() {
    executor.submit(RUNNABLE_DOING_NOTHING);
    executor.submit(RUNNABLE_WITH_ILLEGAL_STATE_EXCEPTION);

    // as the previous runnable should crash with an exception, this task should propagate the exception
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(executor::shutdown)
      .withMessage(ILLEGAL_STATE_EXCEPTION.getMessage());
  }

  @Test
  void interruptShouldBePropagatedOnSubmitNewRunnable() {
    Thread.currentThread().interrupt();
    executor.submit(RUNNABLE_DOING_NOTHING);
    assertThat(Thread.interrupted()).isTrue();
  }

  @Test
  void interruptShouldBePropagatedOnDrainingThreads() {
    Thread.currentThread().interrupt();
    executor.drainThreads();
    assertThat(Thread.interrupted()).isTrue();
  }
}
