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
package org.sonar.plugins.secrets.api.task;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide an intermediately class to work with {@link ExecutorService}.
 * It is dedicated to run tasks that are suspected to cause timeout, it will take care to stop them properly and ensure to keep a valid {@link ExecutorService}.
 * The main entrypoint is the {@link #runRegexMatchingWithTimeout} method.
 */
public class ExecutorServiceManager {
  private static final Logger LOG = LoggerFactory.getLogger(ExecutorServiceManager.class);
  /**
   * The timeout time in millisecond after which the {@link ExecutorServiceManager} will try to interrupt the thread.
   */
  private static int timeoutMs = 10_000;
  /**
   * The timeout time in millisecond after which the {@link ExecutorServiceManager} will stop waiting for the precedent interruption to be effective
   * and will throw a {@link RuntimeException}.
   * Currently set to a very long time (16min) as once it's reached it will kill the analyzer.
   */
  private static int uninterruptibleTimeoutMs = 1_000_000;
  private ExecutorService lastExecutorService;

  public ExecutorServiceManager() {
    lastExecutorService = null;
  }

  private ExecutorService getLastExecutorService() {
    // re-use last executor service if possible, otherwise allocate a new one
    if (lastExecutorService == null || lastExecutorService.isShutdown()) {
      lastExecutorService = Executors.newSingleThreadExecutor();
    }
    return lastExecutorService;
  }

  /**
   * Execute the provided {@link Runnable} and try to interrupt it once {@link ExecutorServiceManager#timeoutMs} number of milliseconds has elapsed.
   * The {@link Runnable} must handle thread interruption properly, otherwise it will break the ongoing and next calls to this method.
   *
   * @param run the task to be executed, it must support the interruption mechanism
   * @param pattern the regex pattern which is being matched against, for logging purposes
   * @param ruleId id of the rule the pattern belongs to
   * @return true if the task was executed within the timeout time, false otherwise
   */
  public boolean runRegexMatchingWithTimeout(Runnable run, String pattern, String ruleId) {
    var executorService = getLastExecutorService();
    Future<?> future = executorService.submit(run);

    try {
      if (waitFutureCompletion(future, ruleId, timeoutMs)) {
        return true;
      }
      executorService.shutdownNow();
      boolean terminationSuccessFull = executorService.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
      if (terminationSuccessFull) {
        return false;
      }
      String patternToDisplay = pattern.replace("\\", "\\\\");
      LOG.error("Couldn't interrupt secret-matching task of rule with id \"{}\", waiting for it to finish. " +
        "Related pattern is \"{}\"",
        ruleId,
        patternToDisplay);
      terminationSuccessFull = executorService.awaitTermination(uninterruptibleTimeoutMs, TimeUnit.MILLISECONDS);
      if (!terminationSuccessFull) {
        throw new RuntimeException(
          String.format("Couldn't interrupt secret-matching task of rule with id \"%s\" after normal timeout(%dms) and interruption timeout(%dms). Related pattern is \"%s\"",
            ruleId,
            timeoutMs,
            uninterruptibleTimeoutMs,
            patternToDisplay));
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  private static boolean waitFutureCompletion(Future<?> future, String ruleId, int timeoutMs) throws ExecutionException, InterruptedException {
    try {
      future.get(timeoutMs, TimeUnit.MILLISECONDS);
      return true;
    } catch (TimeoutException e) {
      LOG.debug(String.format("Timeout secret-matching task of rule with id \"%s\" after %dms.", ruleId, timeoutMs));
      return false;
    }
  }

  public static int getTimeoutMs() {
    return timeoutMs;
  }

  public static void setTimeoutMs(int timeoutMs) {
    if (timeoutMs > 0) {
      ExecutorServiceManager.timeoutMs = timeoutMs;
    }
  }

  public static int getUninterruptibleTimeoutMs() {
    return uninterruptibleTimeoutMs;
  }

  public static void setUninterruptibleTimeoutMs(int uninterruptibleTimeoutMs) {
    if (uninterruptibleTimeoutMs > 0) {
      ExecutorServiceManager.uninterruptibleTimeoutMs = uninterruptibleTimeoutMs;
    }
  }
}
