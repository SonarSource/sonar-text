/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.secrets.api;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.plugins.secrets.api.task.ExecutorServiceManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ExecutorServiceManagerTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  DummyObject dummyObject;
  ExecutorServiceManager executor;

  @AfterEach
  public void cleanUp() {
    int defaultTimeout = 10_000;
    // due to running other tests, this property can be changed. That's why we need to set the default after each test.
    ExecutorServiceManager.setTimeoutMs(defaultTimeout);
    ExecutorServiceManager.setUninterruptibleTimeoutMs(defaultTimeout);
  }

  @BeforeEach
  void setUp() {
    dummyObject = spy(DummyObject.class);
    ExecutorServiceManager.setTimeoutMs(100);
    ExecutorServiceManager.setUninterruptibleTimeoutMs(100);
    executor = new ExecutorServiceManager();
  }

  @Test
  void testMethodCalledNormally() {
    boolean runWithSuccess = executor.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess).isTrue();
    verify(dummyObject).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void testInterruptedMethodNotCalled() {
    boolean runWithSuccess = executor.runRegexMatchingWithTimeout(() -> {
      waitForever();
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess).isFalse();
    verify(dummyObject, never()).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void testNormalCallAfterInterruptedCall() {
    boolean runWithSuccess1 = executor.runRegexMatchingWithTimeout(() -> {
      waitForever();
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");
    boolean runWithSuccess2 = executor.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess1).isFalse();
    assertThat(runWithSuccess2).isTrue();
    verify(dummyObject).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void testTwoConsecutiveNormalCall() {
    boolean runWithSuccess1 = executor.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");
    boolean runWithSuccess2 = executor.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess1).isTrue();
    assertThat(runWithSuccess2).isTrue();
    verify(dummyObject, times(2)).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void testInterruptionPreventedAndNextUseBroken() {
    assertThatThrownBy(() -> executor.runRegexMatchingWithTimeout(() -> {
      waitForeverAndPreventInterruption();
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>"))
      .isInstanceOf(RuntimeException.class)
      .hasMessage(
        "Couldn't interrupt secret-matching task of rule with id \"<rule-id>\" after normal timeout(100ms) and interruption timeout(100ms). Related pattern is \"<pattern-for-logging>\"");

    boolean runWithSuccess2 = executor.runRegexMatchingWithTimeout(() -> {
      dummyObject.method();
    }, "<pattern-for-logging>", "<rule-id>");

    assertThat(runWithSuccess2).isTrue();
    verify(dummyObject, times(1)).method();
    assertThat(logTester.logs())
      .containsExactly("Couldn't interrupt secret-matching task of rule with id \"<rule-id>\", waiting for it to finish. Related pattern is \"<pattern-for-logging>\"");
  }

  @Test
  void testInterruptionOnRunWithTimeout() {
    ExecutorServiceManager.setTimeoutMs(10000);
    ExecutorServiceManager.setUninterruptibleTimeoutMs(10000);
    Throwable t = runAndInterrupt(() -> {
      executor.runRegexMatchingWithTimeout(() -> {
        waitForever();
        dummyObject.method();
      }, "<pattern-for-logging>", "<rule-id>");
    });
    assertThat(t)
      .isInstanceOf(RuntimeException.class)
      .hasMessage("java.lang.InterruptedException");
    verify(dummyObject, never()).method();
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
