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
package org.sonar.plugins.common;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.atIndex;

class MultiFileProgressReportTest {

  private static final Logger LOG = LoggerFactory.getLogger(MultiFileProgressReport.class);

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  @Timeout(5)
  void shouldDisplayMessagePluralized() throws InterruptedException {
    var report = new MultiFileProgressReport(100);
    report.start(3);
    // Wait for start message
    waitForMessage();
    // Wait for at least one progress message
    waitForMessage();

    report.stop();

    assertThat(logTester.logs())
      .contains("3 source files to be analyzed", atIndex(0))
      .contains("0/3 files analyzed, current files: none")
      .last().isEqualTo("3/3 source files have been analyzed");
  }

  @Test
  @Timeout(5)
  void shouldDisplayMessageSingular() throws InterruptedException {
    var report = new MultiFileProgressReport(100);
    report.start(1);
    // Wait for start message
    waitForMessage();
    // Wait for at least one progress message
    waitForMessage();

    report.stop();

    assertThat(logTester.logs())
      .contains("1 source file to be analyzed", atIndex(0))
      .contains("0/1 files analyzed, current files: none")
      .last().isEqualTo("1/1 source file has been analyzed");
  }

  @Test
  @Timeout(5)
  void shouldDisplayMessageForOneCurrentlyAnalyzedFile() throws InterruptedException {
    var report = new MultiFileProgressReport(100);
    report.start(1);
    report.startAnalysisFor("file1");
    // Wait for start message
    waitForMessage();
    // Wait for at least one progress message
    waitForMessage();

    report.stop();

    assertThat(logTester.logs())
      .contains("1 source file to be analyzed", atIndex(0))
      .contains("0/1 files analyzed, current file: file1")
      .last().isEqualTo("1/1 source file has been analyzed");
  }

  @Test
  @Timeout(5)
  void shouldDisplayMessageForTwoCurrentlyAnalyzedFiles() throws InterruptedException {
    var report = new MultiFileProgressReport(100);
    report.start(2);
    report.startAnalysisFor("file1");
    report.startAnalysisFor("file2");
    // Wait for start message
    waitForMessage();
    // Wait for at least one progress message
    waitForMessage();

    report.stop();

    assertThat(logTester.logs())
      .contains("2 source files to be analyzed", atIndex(0))
      .contains("0/2 files analyzed, current files: file1, file2")
      .last().isEqualTo("2/2 source files have been analyzed");
  }

  @Test
  @Timeout(5)
  void shouldDisplayMessageForTwoCurrentlyAnalyzedFilesWhenOneAlreadyFinished() throws InterruptedException {
    var report = new MultiFileProgressReport(100);
    report.start(3);
    report.startAnalysisFor("file1");
    report.startAnalysisFor("file2");
    report.startAnalysisFor("file3");
    report.finishAnalysisFor("file2");
    // Wait for start message
    waitForMessage();
    // Wait for at least one progress message
    waitForMessage();

    report.stop();

    assertThat(logTester.logs())
      .contains("3 source files to be analyzed", atIndex(0))
      .contains("1/3 files analyzed, current files: file1, file3")
      .last().isEqualTo("3/3 source files have been analyzed");
  }

  @Test
  @Timeout(5)
  void shouldAbbreviateLogMessageInInfoLogLevel() throws InterruptedException {
    var report = new MultiFileProgressReport(100);
    report.start(4);
    report.startAnalysisFor("file1");
    report.startAnalysisFor("file2");
    report.startAnalysisFor("file3");
    report.startAnalysisFor("file4");
    // Wait for start message
    waitForMessage();
    // Wait for at least one progress message
    waitForMessage();

    report.stop();

    assertThat(logTester.logs())
      .contains("4 source files to be analyzed", atIndex(0))
      .contains("0/4 files analyzed, current files: file1, file2, file3, ...")
      .last().isEqualTo("4/4 source files have been analyzed");
  }

  @Test
  @Timeout(5)
  void shouldNotAbbreviateLogMessageInInfoLogLevel() throws InterruptedException {
    var report = new MultiFileProgressReport(100);
    logTester.setLevel(Level.DEBUG);
    report.start(4);
    report.startAnalysisFor("file1");
    report.startAnalysisFor("file2");
    report.startAnalysisFor("file3");
    report.startAnalysisFor("file4");
    // Wait for start message
    waitForMessage();
    // Wait for at least one progress message
    waitForMessage();

    report.stop();

    assertThat(logTester.logs())
      .contains("4 source files to be analyzed", atIndex(0))
      .contains("0/4 files analyzed, current files: file1, file2, file3, file4")
      .last().isEqualTo("4/4 source files have been analyzed");
  }

  @Test
  @Timeout(5)
  void shouldDisplayLogWhenExceedingInitialNumberOfAnalyzedFiles() throws InterruptedException {
    var report = new MultiFileProgressReport(100);
    logTester.setLevel(Level.DEBUG);
    report.start(2);
    // Wait for start message
    waitForMessage();
    report.startAnalysisFor("file1");
    report.finishAnalysisFor("file1");
    report.startAnalysisFor("file2");
    report.finishAnalysisFor("file2");
    report.startAnalysisFor("fileThatExceedsSize");
    report.finishAnalysisFor("fileThatExceedsSize");
    // Wait for at least one progress message
    waitForMessage();

    report.stop();

    assertThat(logTester.logs())
      .contains("2 source files to be analyzed", atIndex(0))
      .contains("2/2 files analyzed, current files: none",
        "Reported finished analysis on more files than expected")
      .last().isEqualTo("2/2 source files have been analyzed");
  }

  @Test
  @Timeout(5)
  void shouldDisplayLogWhenFinishingAnalysisOnNotStartedFile() throws InterruptedException {
    var report = new MultiFileProgressReport(100);
    logTester.setLevel(Level.DEBUG);
    report.start(2);
    report.finishAnalysisFor("file1");
    // Wait for start message
    waitForMessage();
    // Wait for at least one progress message
    waitForMessage();

    report.stop();

    assertThat(logTester.logs()).contains("2 source files to be analyzed")
      .contains("0/2 files analyzed, current files: none",
        "Couldn't finish progress report of file \"file1\", as it was not in the list of files being analyzed")
      .last().isEqualTo("2/2 source files have been analyzed");
  }

  @Test
  @Timeout(5)
  void shouldCancelCorrectly() throws InterruptedException {
    var report = new MultiFileProgressReport(100);
    report.start(1);
    // Wait for start message
    waitForMessage();

    report.cancel();

    assertThat(logTester.logs()).containsExactly(
      "1 source file to be analyzed");
  }

  @Test
  @Timeout(5)
  void shouldPreserveInterruptFlagOnStop() throws InterruptedException {
    var report = new MultiFileProgressReport(100);
    report.start(1);
    // Wait for start message
    waitForMessage();

    AtomicBoolean interruptFlagPreserved = new AtomicBoolean(false);

    Thread t = new Thread(() -> {
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e1) {
        Thread.currentThread().interrupt();
      }
      report.stop();
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        interruptFlagPreserved.set(true);
      }
    });
    t.start();
    t.interrupt();
    t.join(1000);
    waitForMessage();
    assertThat(interruptFlagPreserved.get()).isTrue();

    assertThat(logTester.logs()).contains("1/1 source file has been analyzed");
  }

  @Test
  @Timeout(1)
  void interruptingTheThreadShouldNeverCreateADeadlock() {
    var report = new MultiFileProgressReport();
    long start = System.currentTimeMillis();
    report.start(0);
    report.stop();
    long end = System.currentTimeMillis();

    // stopping the report too soon could fail to interrupt the thread that was not yet alive,
    // and fail to set the proper state for Thread.interrupted()
    // this test ensures that the report does not loop once or is interrupted when stop() is
    // called just after start()
    assertThat(end - start).isLessThan(300);
  }

  @Test
  @Timeout(1)
  void interruptedThreadShouldExitImmediately() throws InterruptedException {
    var report = new MultiFileProgressReport();
    AtomicLong time = new AtomicLong(10000);
    Thread selfInterruptedThread = new Thread(() -> {
      // set the thread as interrupted
      Thread.currentThread().interrupt();
      long start = System.currentTimeMillis();
      // execute run, while the thread is interrupted
      report.run();
      long end = System.currentTimeMillis();
      time.set(end - start);
    });
    selfInterruptedThread.start();
    selfInterruptedThread.join();
    assertThat(time.get()).isLessThan(300);
  }

  @Test
  @Timeout(10)
  void shouldNotThrowConcurrentModificationException() {
    // disable debug logs to reduce noise
    logTester.setLevel(Level.ERROR);
    var progressUpdatePeriodMillis = 10;
    var report = new MultiFileProgressReport(progressUpdatePeriodMillis);
    var numFiles = 500;
    report.start(numFiles);

    var progressUpdateDurationSeconds = 5;
    var progressUpdater = new Thread(() -> {
      for (int s = 0; s < progressUpdateDurationSeconds * 1000 / progressUpdatePeriodMillis; s++) {
        for (int i = 0; i < numFiles; i++) {
          report.finishAnalysisFor("newFile#" + i);
          report.startAnalysisFor("newFile#" + i);
        }
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      report.stop();
    });

    progressUpdater.start();
    assertThatNoException().isThrownBy(report::run);

    logTester.setLevel(Level.DEBUG);
  }

  private static void waitForMessage() throws InterruptedException {
    synchronized (LOG) {
      LOG.wait(1000);
    }
  }
}
