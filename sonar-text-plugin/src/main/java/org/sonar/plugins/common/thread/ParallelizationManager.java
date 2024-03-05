/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
package org.sonar.plugins.common.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParallelizationManager {

  private static final Logger LOG = LoggerFactory.getLogger(ParallelizationManager.class);
  private static final int SEMAPHORE_ACQUIRE_TIMEOUT = 100;
  private final int threads;
  private final ExecutorService executorService;
  private final Semaphore semaphore;
  private final AtomicReference<Exception> exception = new AtomicReference<>();

  public ParallelizationManager(int threads) {
    this.threads = threads;
    this.executorService = Executors.newFixedThreadPool(threads);
    semaphore = new Semaphore(threads);
  }

  public void submit(Runnable runnable) {
    if (executorService.isShutdown()) {
      throw new RejectedExecutionException();
    }

    try {
      boolean acquired;
      do {
        acquired = semaphore.tryAcquire(SEMAPHORE_ACQUIRE_TIMEOUT, TimeUnit.MILLISECONDS);
        propagatePossibleException();
      } while (!acquired);

      executorService.execute(() -> {
        try {
          runnable.run();
          semaphore.release();
        } catch (Exception e) {
          LOG.warn("Exception in thread " + Thread.currentThread().getName(), e);
          exception.getAndSet(e);
        }
      });
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void drainThreads() {
    try {
      boolean acquired;
      do {
        acquired = semaphore.tryAcquire(threads, SEMAPHORE_ACQUIRE_TIMEOUT, TimeUnit.MILLISECONDS);
        propagatePossibleException();
      } while (!acquired);
      semaphore.release(threads);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void shutdown() {
    executorService.shutdown();
    try {
      // we don't know how long threads will take to finish, so we wait for a long time
      executorService.awaitTermination(1, TimeUnit.HOURS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    propagatePossibleException();
  }

  private void propagatePossibleException() {
    Throwable e = exception.getAndSet(null);
    if (e != null) {
      if (e instanceof IllegalStateException illegalStateException) {
        throw illegalStateException;
      }
      throw new IllegalStateException(e);
    }
  }
}
