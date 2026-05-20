/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.secrets.api.filters;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emits debug log lines describing why a candidate was rejected during post filtering.
 *
 * <p>The logger is internal: gated by {@code sonar.text.debug.logRejectedCandidates} (sonar property or
 * system property of the same name). When disabled, all calls are no-ops; when enabled, output is
 * rate-limited per {@code (ruleId, file)} pair to keep noise bounded on files heavily using a provider.
 *
 * <p>The log line carries the rule id, the file, the offset of the candidate match, and a handler-provided
 * description of the rejection — the filter name plus any specifics that help a specifier locate the mis-tuned
 * setting (e.g. {@code "patternNot: <pattern>"}, {@code "statistical (entropy): entropy=2.710, threshold=3.500"}).
 * The description never includes the candidate text.
 */
public final class RejectionLogger {

  private static final Logger LOG = LoggerFactory.getLogger(RejectionLogger.class);

  /** Singleton no-op instance used by tests and the default path. */
  public static final RejectionLogger DISABLED = new RejectionLogger(false, 0);

  private final boolean enabled;
  private final int maxPerRulePerFile;
  // Lifetime: this logger is instantiated once per SpecificationConfiguration (i.e. per analysis run), so the map
  // grows only within a single scan and is released when the run ends. No periodic clean-up needed.
  private final ConcurrentMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

  RejectionLogger(boolean enabled, int maxPerRulePerFile) {
    this.enabled = enabled;
    this.maxPerRulePerFile = maxPerRulePerFile;
  }

  public static RejectionLogger create(int maxPerRulePerFile) {
    LOG.info("Post-filter rejection logging is enabled (max {} entries per rule per file)", maxPerRulePerFile);
    return new RejectionLogger(true, Math.max(1, maxPerRulePerFile));
  }

  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Log a rejection for {@code context}. No-op when the logger is disabled, when {@code context} has
   * no file (e.g., {@link RejectionLogContext#NONE}), or when the per-(rule, file) limit has been
   * exceeded. When the limit is reached, a single summary line is emitted so the user knows further
   * rejections were suppressed.
   *
   * @param context request context built by the caller (rule id, file, offset)
   * @param details handler-provided description of the rejection (filter name plus any safe metadata such as
   *                {@code "patternNot: index=1"}); must not include the candidate text or rule patterns
   */
  public void log(RejectionLogContext context, String details) {
    if (!enabled || context.isEmpty()) {
      return;
    }

    var key = counterKey(context);
    var counter = counters.computeIfAbsent(key, k -> new AtomicInteger());
    int current = counter.incrementAndGet();

    if (current <= maxPerRulePerFile) {
      LOG.debug("Rejecting candidate [rule={}, file={}, offset={}] by {}",
        context.ruleId(),
        context.fileContext(),
        context.matchStartOffset(),
        details);
    } else if (current == maxPerRulePerFile + 1) {
      LOG.debug("Further candidate rejections suppressed for [rule={}, file={}] after {} entries",
        context.ruleId(),
        context.fileContext(),
        maxPerRulePerFile);
    }
  }

  private static String counterKey(RejectionLogContext context) {
    return context.ruleId() + "||" + context.fileContext();
  }
}
