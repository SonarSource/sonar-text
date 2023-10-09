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

public class ExecutorServiceManager {
  private static final Logger LOG = LoggerFactory.getLogger(ExecutorServiceManager.class);
  public static int timeoutMs = 10000;
  public static int uninterruptibleTimeoutMs = 10000;
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

  public boolean runWithTimeout(Runnable run) {
    var executorService = getLastExecutorService();
    Future<?> future = executorService.submit(run);

    try {
      future.get(timeoutMs, TimeUnit.MILLISECONDS);
      return true;
    } catch (TimeoutException e) {
      executorService.shutdownNow();
      try {
        if (!executorService.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
          LOG.error("Couldn't interrupt task, waiting for it to finish...");
          if (!executorService.awaitTermination(uninterruptibleTimeoutMs, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException(String.format("Couldn't interrupt task after normal timeout(%dms) and interruption timeout(%dms).", timeoutMs, uninterruptibleTimeoutMs));
          }
        }
      } catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    return false;
  }
}
