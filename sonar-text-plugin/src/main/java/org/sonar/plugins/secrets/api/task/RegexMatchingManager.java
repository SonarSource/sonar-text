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
package org.sonar.plugins.secrets.api.task;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide an intermediately class to work with {@link ExecutorService}.
 * It is dedicated to run tasks that are suspected to cause timeout, it will take care to stop them properly and ensure to keep a valid {@link ExecutorService}.
 * The main entrypoint is the {@link #runRegexMatchingWithTimeout} method.
 */
public class RegexMatchingManager {
  private static final Logger LOG = LoggerFactory.getLogger(RegexMatchingManager.class);
  /**
   * The timeout time in millisecond after which the {@link RegexMatchingManager} will try to interrupt the thread.
   */
  private static int timeoutMs = 10_000;
  /**
   * The timeout time in millisecond after which the {@link RegexMatchingManager} will stop waiting for the precedent interruption to be effective
   * and will throw a {@link RuntimeException}. Once it's reached it will kill the analysis of this file.
   */
  private static int uninterruptibleTimeoutMs = 60_000;

  // This needs to be set to actual number of threads that can run an analysis in parallel
  private static int threadCount = Runtime.getRuntime().availableProcessors();
  protected static ExecutorService executorService;

  private RegexMatchingManager() {
  }

  /**
   * Initialize the {@link RegexMatchingManager} with the provided number of threads.
   * If the provided number is less than 1, the default number of threads will be used.
   * This method needs to be called before any call to {@link RegexMatchingManager#runRegexMatchingWithTimeout}.
   *
   * @param threads the number of threads to be used by the {@link RegexMatchingManager}
   */
  public static synchronized void initialize(int threads) {
    if (threads > 0) {
      threadCount = threads;
    }
    executorService = Executors.newFixedThreadPool(threadCount);
  }

  /**
   * Execute the provided {@link Runnable} and try to interrupt it once {@link RegexMatchingManager#timeoutMs} number of milliseconds has elapsed.
   * The {@link Runnable} must handle thread interruption properly, otherwise it will break the ongoing and next calls to this method.
   *
   * @param runnable the task to be executed, it must support the interruption mechanism
   * @param pattern the regex pattern which is being matched against, for logging purposes
   * @param ruleId id of the rule the pattern belongs to
   * @return true if the task was executed within the timeout time, false otherwise
   */
  public static boolean runRegexMatchingWithTimeout(Runnable runnable, String pattern, String ruleId) {
    forceInitialization();
    var semaphore = new Semaphore(0);
    Future<?> future = submitWithSemaphore(runnable, semaphore);

    try {
      if (waitFutureCompletion(future, ruleId, timeoutMs)) {
        return true;
      }
      future.cancel(true);

      boolean terminationSuccessFull = semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
      if (terminationSuccessFull) {
        return false;
      }

      String patternToDisplay = pattern.replace("\\", "\\\\");
      LOG.warn("Couldn't interrupt secret-matching task of rule with id \"{}\", waiting for it to finish. " +
        "Related pattern is \"{}\"",
        ruleId,
        patternToDisplay);

      terminationSuccessFull = semaphore.tryAcquire(uninterruptibleTimeoutMs, TimeUnit.MILLISECONDS);
      if (!terminationSuccessFull) {
        reinitialize();
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

  private static Future<?> submitWithSemaphore(Runnable runnable, Semaphore semaphore) {
    return executorService.submit(() -> {
      try {
        runnable.run();
      } catch (RuntimeException e) {
        throw e;
      } finally {
        semaphore.release();
      }
    });
  }

  private static boolean waitFutureCompletion(Future<?> future, String ruleId, int timeoutMs) throws ExecutionException, InterruptedException {
    try {
      future.get(timeoutMs, TimeUnit.MILLISECONDS);
      return true;
    } catch (TimeoutException e) {
      LOG.debug("Timeout secret-matching task of rule with id \"{}\" after {}ms.", ruleId, timeoutMs);
      return false;
    }
  }

  /**
   * Reinitialize the {@link RegexMatchingManager} with the current number of threads.
   * This method is useful to ensure that the {@link RegexMatchingManager} will be able to work properly after a regex matching timed out.
   */
  public static synchronized void reinitialize() {
    var oldExecutorService = executorService;
    initialize(threadCount);
    oldExecutorService.shutdown();
  }

  /**
   * Forces an initialization of the {@link RegexMatchingManager} with the current number of threads if the executorService is null.
   */
  public static synchronized void forceInitialization() {
    if (executorService == null || executorService.isShutdown()) {
      initialize(threadCount);
    }
  }

  /**
   * Shutdown the {@link RegexMatchingManager}.
   */
  public static void shutdown() {
    executorService.shutdown();
  }

  public static int getTimeoutMs() {
    return timeoutMs;
  }

  public static void setTimeoutMs(int timeoutMs) {
    if (timeoutMs > 0) {
      RegexMatchingManager.timeoutMs = timeoutMs;
    }
  }

  public static int getUninterruptibleTimeoutMs() {
    return uninterruptibleTimeoutMs;
  }

  public static void setUninterruptibleTimeoutMs(int uninterruptibleTimeoutMs) {
    if (uninterruptibleTimeoutMs > 0) {
      RegexMatchingManager.uninterruptibleTimeoutMs = uninterruptibleTimeoutMs;
    }
  }
}
