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

  @BeforeEach
  void setUp() {
    dummyObject = spy(DummyObject.class);
    ExecutorServiceManager.timeoutMs = 100;
    ExecutorServiceManager.uninterruptibleTimeoutMs = 100;
    executor = new ExecutorServiceManager();
  }

  @Test
  void testMethodCalledNormally() {
    boolean runWithSuccess = executor.runWithTimeout(() -> {
      dummyObject.method();
    });

    assertThat(runWithSuccess).isTrue();
    verify(dummyObject).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void testInterruptedMethodNotCalled() {
    boolean runWithSuccess = executor.runWithTimeout(() -> {
      waitForever();
      dummyObject.method();
    });

    assertThat(runWithSuccess).isFalse();
    verify(dummyObject, never()).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void testNormalCallAfterInterruptedCall() {
    boolean runWithSuccess1 = executor.runWithTimeout(() -> {
      waitForever();
      dummyObject.method();
    });
    boolean runWithSuccess2 = executor.runWithTimeout(() -> {
      dummyObject.method();
    });

    assertThat(runWithSuccess1).isFalse();
    assertThat(runWithSuccess2).isTrue();
    verify(dummyObject).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void testTwoConsecutiveNormalCall() {
    boolean runWithSuccess1 = executor.runWithTimeout(() -> {
      dummyObject.method();
    });
    boolean runWithSuccess2 = executor.runWithTimeout(() -> {
      dummyObject.method();
    });

    assertThat(runWithSuccess1).isTrue();
    assertThat(runWithSuccess2).isTrue();
    verify(dummyObject, times(2)).method();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  void testInterruptionPreventedAndNextUseBroken() {
    assertThatThrownBy(() -> executor.runWithTimeout(() -> {
      waitForeverAndPreventInterruption();
      dummyObject.method();
    }))
      .isInstanceOf(RuntimeException.class)
      .hasMessage("Couldn't interrupt task after normal timeout(100ms) and interruption timeout(100ms).");

    boolean runWithSuccess2 = executor.runWithTimeout(() -> {
      dummyObject.method();
    });

    assertThat(runWithSuccess2).isTrue();
    verify(dummyObject, times(1)).method();
    assertThat(logTester.logs()).containsExactly("Couldn't interrupt task, waiting for it to finish...");
  }

  @Test
  void testInterruptionOnRunWithTimeout() {
    ExecutorServiceManager.timeoutMs = 10000;
    ExecutorServiceManager.uninterruptibleTimeoutMs = 10000;
    Throwable t = runAndInterrupt(() -> {
      executor.runWithTimeout(() -> {
        waitForever();
        dummyObject.method();
      });
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
