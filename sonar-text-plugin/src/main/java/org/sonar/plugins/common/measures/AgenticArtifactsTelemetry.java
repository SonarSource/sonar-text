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
package org.sonar.plugins.common.measures;

import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.common.predicates.TextAndSecretsPredicates;

public class AgenticArtifactsTelemetry {

  record AgenticPattern(String category, String tool, String pattern) {
  }

  static final String SKILL_FILES = "skill_files";
  static final String INSTRUCTIONS_FILES = "instructions_files";
  static final String MCP_CONFIG_FILES = "mcp_config_files";
  static final String OTHER_AGENT_FILES = "other_agent_files";

  private static final String TOOL_CLAUDE_CODE = "claude-code";
  private static final String TOOL_CURSOR = "cursor";
  private static final String TOOL_WINDSURF = "windsurf";
  private static final String TOOL_AIDER = "aider";
  private static final String TOOL_CONTINUE = "continue";
  private static final String TOOL_CLINE = "cline";
  private static final String TOOL_GITHUB_COPILOT = "github-copilot";
  private static final String TOOL_CODEX = "codex";
  private static final String TOOL_VENDOR_NEUTRAL = "vendor-neutral";
  private static final String TOOL_GEMINI = "gemini";

  // Patterns are evaluated in declaration order: predicates.or(...) short-circuits on the first
  // match, so overlapping patterns are allowed as long as the more specific one is listed first
  // (e.g. .claude/skills/**/SKILL.md before the vendor-neutral **/skills/**/SKILL.md). Each file
  // still matches at most one pattern and is counted under a single category/tool.
  static final List<AgenticPattern> PATTERNS = List.of(
    new AgenticPattern(SKILL_FILES, TOOL_CLAUDE_CODE, "**/.claude/skills/**/SKILL.md"),
    new AgenticPattern(SKILL_FILES, TOOL_VENDOR_NEUTRAL, "**/skills/**/SKILL.md"),
    new AgenticPattern(INSTRUCTIONS_FILES, TOOL_CLAUDE_CODE, "CLAUDE.md"),
    new AgenticPattern(INSTRUCTIONS_FILES, TOOL_CODEX, "**/.codex/AGENTS.md"),
    new AgenticPattern(INSTRUCTIONS_FILES, TOOL_CODEX, "**/.codex/AGENTS.override.md"),
    new AgenticPattern(INSTRUCTIONS_FILES, TOOL_VENDOR_NEUTRAL, "AGENTS.md"),
    new AgenticPattern(INSTRUCTIONS_FILES, TOOL_GEMINI, "GEMINI.md"),
    new AgenticPattern(INSTRUCTIONS_FILES, TOOL_CURSOR, "**/.cursor/rules/**"),
    new AgenticPattern(INSTRUCTIONS_FILES, TOOL_CURSOR, ".cursorrules"),
    new AgenticPattern(INSTRUCTIONS_FILES, TOOL_WINDSURF, ".windsurfrules"),
    new AgenticPattern(MCP_CONFIG_FILES, TOOL_CLAUDE_CODE, "**/.mcp.json"),
    new AgenticPattern(MCP_CONFIG_FILES, TOOL_CURSOR, "**/.cursor/mcp.json"),
    new AgenticPattern(OTHER_AGENT_FILES, TOOL_AIDER, ".aider.conf.yml"),
    new AgenticPattern(OTHER_AGENT_FILES, TOOL_CONTINUE, "**/.continue/**"),
    new AgenticPattern(OTHER_AGENT_FILES, TOOL_CLINE, "**/.clinerules"),
    new AgenticPattern(OTHER_AGENT_FILES, TOOL_GITHUB_COPILOT, "**/.github/copilot-instructions.md"),
    new AgenticPattern(OTHER_AGENT_FILES, TOOL_CODEX, "**/.codex/config.toml"));

  // Derived from PATTERNS so a category can never drift out of sync with the patterns that define it.
  static final List<String> CATEGORIES = PATTERNS.stream().map(AgenticPattern::category).distinct().toList();

  private AgenticArtifactsTelemetry() {
    // only static methods
  }

  public static void measureAgenticArtifacts(SensorContext sensorContext, TelemetryReporter telemetryReporter) {
    if (!TextAndSecretsPredicates.isHiddenFilesAnalysisSupported(sensorContext.runtime())) {
      // Guarantees that telemetry is not calculated in SQ-IDE context since telemetry won't be saved there.
      // Since most agentic artifacts are dotfiles/dot-directories, we only calculate this when hidden file analysis is supported
      return;
    }

    var fileSystem = sensorContext.fileSystem();
    var predicates = fileSystem.predicates();

    var filesPerCategory = new HashMap<String, Integer>();
    CATEGORIES.forEach(category -> filesPerCategory.put(category, 0));
    var detectedTools = new TreeSet<String>();

    var recordingPredicates = PATTERNS.stream()
      .<FilePredicate>map(pattern -> new RecordingPredicate(predicates.matchesPathPattern(pattern.pattern()),
        file -> {
          filesPerCategory.merge(pattern.category(), 1, Integer::sum);
          detectedTools.add(pattern.tool());
        }))
      .toList();
    var combinedPredicate = predicates.or(recordingPredicates);

    // predicates.or(...) short-circuits on the first predicate match, so PATTERNS order is
    // significant: an overlapping file is counted once, under the earliest matching pattern.
    // Iterating triggers RecordingPredicate's counting side effects
    fileSystem.inputFiles(combinedPredicate).forEach(inputFile -> {
      // no-op: counting already happened inside RecordingPredicate.apply()
    });

    CATEGORIES.forEach(category -> telemetryReporter.addNumericMeasure("agentic_" + category + "_count", filesPerCategory.get(category)));

    if (!detectedTools.isEmpty()) {
      telemetryReporter.addListAsStringMeasure("agentic_tools", detectedTools);
    }
  }
}
