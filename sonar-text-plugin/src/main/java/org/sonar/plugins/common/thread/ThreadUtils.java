/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource Sàrl
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
package org.sonar.plugins.common.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThreadUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ThreadUtils.class);

  private ThreadUtils() {
    // util class
  }

  public static void setThreadName(Thread thread, String name) {
    try {
      thread.setName(name);
    } catch (SecurityException e) {
      // If the SecurityManager (deprecated in Java 17) is used, it may potentially throw SecurityException.
      var message = "Unable to change thread name from \"%s\" to \"%s\"".formatted(Thread.currentThread().getName(), name);
      LOG.warn(message, e);
    }
  }
}
