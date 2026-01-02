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
package org.sonar.plugins.common;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.common.thread.ThreadUtils;

public class MultiFileProgressReport implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(MultiFileProgressReport.class);
  private static final int MAX_NUMBER_OF_FILES_TO_DISPLAY = 3;
  private static final int DEFAULT_PROGRESS_UPDATE_PERIOD_MILLIS = 10000;

  // data structure is chosen because of the preservation of insertion order. This allows us to display the longest running files first.
  private final Collection<String> currentFileNames = new ConcurrentLinkedDeque<>();
  private final Thread thread;
  private final long progressUpdatePeriod;
  private final String analysisName;
  private long size;
  private long numberOfFinishedFiles;
  private boolean success;

  /**
   * The report loop can not rely only on Thread.interrupted() to end, according to
   * interrupted() javadoc, a thread interruption can be ignored because a thread was
   * not alive at the time of the interrupt. This could happen if stop() is being called
   * before ProgressReport's thread becomes alive.
   * So this boolean flag ensures that ProgressReport never enter an infinite loop when
   * Thread.interrupted() failed to be set to true.
   */
  private final AtomicBoolean interrupted = new AtomicBoolean();

  public MultiFileProgressReport(String analysisName) {
    this(DEFAULT_PROGRESS_UPDATE_PERIOD_MILLIS, analysisName);
  }

  public MultiFileProgressReport(long progressUpdatePeriod, String analysisName) {
    this.progressUpdatePeriod = progressUpdatePeriod;
    this.analysisName = analysisName;
    interrupted.set(false);
    thread = new Thread(this);
    ThreadUtils.setThreadName(thread, "Progress of the %s".formatted(analysisName));
    thread.setDaemon(true);
    thread.setUncaughtExceptionHandler((thread, throwable) -> LOG.debug("Uncaught exception in the progress report thread: {}", throwable.getClass().getCanonicalName()));
  }

  @Override
  public void run() {
    LOG.info("{} source {} to be analyzed for the {}", size, pluralizeFile(size), analysisName);
    while (!(interrupted.get() || Thread.currentThread().isInterrupted())) {
      try {
        Thread.sleep(progressUpdatePeriod);
        logCurrentProgress();
      } catch (InterruptedException e) {
        interrupted.set(true);
        thread.interrupt();
        break;
      }
    }
    if (success) {
      LOG.info("{}/{} source {} {} been analyzed for the {}", size, size, pluralizeFile(size), pluralizeHas(size), analysisName);
    }
  }

  private static String pluralizeFile(long count) {
    if (count == 1L) {
      return "file";
    }
    return "files";
  }

  private static String pluralizeHas(long count) {
    if (count == 1L) {
      return "has";
    }
    return "have";
  }

  public synchronized void start(int size) {
    this.size = size;
    thread.start();
  }

  public void startAnalysisFor(String fileName) {
    currentFileNames.add(fileName);
  }

  public synchronized void finishAnalysisFor(String fileName) {
    if (!currentFileNames.remove(fileName)) {
      LOG.debug("Couldn't finish progress report of file \"{}\", as it was not in the list of files being analyzed", fileName);
      return;
    }
    if (numberOfFinishedFiles < size) {
      numberOfFinishedFiles++;
    } else {
      LOG.debug("Reported finished analysis on more files than expected");
    }
  }

  public synchronized void stop() {
    interrupted.set(true);
    success = true;
    thread.interrupt();
    join();
  }

  public synchronized void cancel() {
    interrupted.set(true);
    thread.interrupt();
    join();
  }

  private void join() {
    try {
      thread.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void logCurrentProgress() {
    var sb = new StringBuilder();
    Collection<String> currentFileNamesCopy;
    synchronized (this) {
      currentFileNamesCopy = new LinkedHashSet<>(currentFileNames);
    }
    int numberOfFiles = currentFileNamesCopy.size();
    sb.append(numberOfFinishedFiles)
      .append("/")
      .append(size)
      .append(" files analyzed, current ")
      .append(pluralizeFile(numberOfFiles))
      .append(": ");

    boolean debugEnabled = LOG.isDebugEnabled();
    if (numberOfFiles == 0) {
      sb.append("none");
    } else {
      int numberOfFilesToDisplay = debugEnabled ? numberOfFiles : Math.min(numberOfFiles, MAX_NUMBER_OF_FILES_TO_DISPLAY);
      var fileNamesToDisplay = currentFileNamesCopy.stream()
        .limit(numberOfFilesToDisplay)
        .collect(Collectors.joining(", "));
      sb.append(fileNamesToDisplay);
      if (numberOfFiles > numberOfFilesToDisplay) {
        sb.append(", ...");
      }
    }

    logSynchronized(sb.toString(), debugEnabled);
  }

  private static void logSynchronized(String message, boolean debug) {
    synchronized (LOG) {
      if (debug) {
        LOG.debug(message);
      } else {
        LOG.info(message);
      }
      LOG.notifyAll();
    }
  }
}
