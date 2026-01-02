/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.secrets.api.task;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RegexMatchingManagerTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  DummyObject dummyObject;

  @AfterEach
  public void cleanUp() {
    int defaultTimeout = 10_000;
    // due to running other tests, this property can be changed. That's why we need to set the default after each test.
    RegexMatchingManager.setTimeoutMs(defaultTimeout);
    RegexMatchingManager.setUninterruptibleTimeoutMs(defaultTimeout);
  }

  @BeforeEach
  void setUp() {
    dummyObject = spy(DummyObject.class);
    RegexMatchingManager.setTimeoutMs(100);
    RegexMatchingManager.setUninterruptibleTimeoutMs(100);
    RegexMatchingManager.initialize(Runtime.getRuntime().availableProcessors());
  }

  @Test
  void testMethodCalledNormally() {
    boolean runWithSuccess = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess).isTrue();
    verify(dummyObject).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void testInterruptedMethodNotCalled() {
    boolean runWithSuccess = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      waitForever();
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess).isFalse();
    verify(dummyObject, never()).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void testNormalCallAfterInterruptedCall() {
    boolean runWithSuccess1 = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      waitForever();
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");
    boolean runWithSuccess2 = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess1).isFalse();
    assertThat(runWithSuccess2).isTrue();
    verify(dummyObject).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void testTwoConsecutiveNormalCall() {
    boolean runWithSuccess1 = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");
    boolean runWithSuccess2 = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess1).isTrue();
    assertThat(runWithSuccess2).isTrue();
    verify(dummyObject, times(2)).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void testInterruptionPreventedAndNextUseBroken() {
    assertThatThrownBy(() -> RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      waitForeverAndPreventInterruption();
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>"))
      .isInstanceOf(RuntimeException.class)
      .hasMessage(
        "Couldn't interrupt secret-matching task of rule with id \"<rule-id>\" after normal timeout(100ms) and interruption timeout(100ms). Related pattern is \"<pattern-for-logging>\"");

    boolean runWithSuccess2 = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess2).isTrue();
    verify(dummyObject, times(1)).method();
    assertThat(logTester.logs())
      .containsExactly("Couldn't interrupt secret-matching task of rule with id \"<rule-id>\", waiting for it to finish. Related pattern is \"<pattern-for-logging>\"");
  }

  @Test
  void testInterruptionOnRunWithTimeout() {
    RegexMatchingManager.setTimeoutMs(10000);
    RegexMatchingManager.setUninterruptibleTimeoutMs(10000);
    Throwable t = runAndInterrupt(() -> {
      RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
        waitForever();
        dummyObject.method();
      }, "<pattern-for-logging>", "<rule-id>");
    });
    assertThat(t)
      .isInstanceOf(RuntimeException.class)
      .hasMessage("java.lang.InterruptedException");
    verify(dummyObject, never()).method();
  }

  @Test
  void shouldInitializeNewExecutorServiceWhenPreviousIsShutdown() {
    RegexMatchingManager.shutdown();
    boolean runWithSuccess = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess).isTrue();
    verify(dummyObject).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldInitializeNewExecutorServiceWhenExecutorServiceIsNull() {
    RegexMatchingManager.executorService = null;
    boolean runWithSuccess = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess).isTrue();
    verify(dummyObject).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldInitializeWorkingExecutorServiceOnNegativeThreadNumber() {
    RegexMatchingManager.executorService = null;
    RegexMatchingManager.initialize(-1);
    boolean runWithSuccess = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess).isTrue();
    verify(dummyObject).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void shouldSuccessfullyAcquireSemaphoreDuringSecondTry() {
    RegexMatchingManager.setTimeoutMs(1);
    RegexMatchingManager.setUninterruptibleTimeoutMs(10000);
    boolean runWithSuccess = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      try {
        // should be interrupted by future.cancel(true)
        Thread.currentThread().join();
      } catch (InterruptedException e) {
        try {
          // sleeps during the first semaphore.tryAcquire(), but not during the second
          Thread.sleep(100);
        } catch (InterruptedException ex) {
          // do nothing
        }
      }
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess).isFalse();
    assertThat(logTester.logs()).contains(
      "Couldn't interrupt secret-matching task of rule with id \"<rule-id>\", waiting for it to finish. Related pattern is \"<pattern-for-logging>\"");
  }

  static class DummyObject {
    void method() {
    }
  }

  void waitForever() {
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  void waitForeverAndPreventInterruption() {
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      waitForever();
    }
  }

  Throwable runAndInterrupt(Runnable run) {
    AtomicReference<Throwable> exception = new AtomicReference<>();
    Thread thread = new Thread(run);
    thread.setUncaughtExceptionHandler((th, ex) -> exception.set(ex));
    thread.start();
    thread.interrupt();
    try {
      thread.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return exception.get();
  }
}
