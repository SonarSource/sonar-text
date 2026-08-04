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

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.plugins.common.TestUtils.SONARLINT_RUNTIME;
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME;
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME_WITHOUT_HIDDEN_FILES_SUPPORT;
import static org.sonar.plugins.common.TestUtils.inputFile;

class AgenticArtifactsTelemetryTest {

  @Test
  void everyPatternShouldHaveNonBlankFieldsAndAKnownCategory() {
    for (AgenticArtifactsTelemetry.AgenticPattern pattern : AgenticArtifactsTelemetry.PATTERNS) {
      assertThat(pattern.category()).isIn(AgenticArtifactsTelemetry.CATEGORIES);
      assertThat(pattern.tool()).isNotBlank();
      assertThat(pattern.pattern()).isNotBlank();
    }
  }

  @Test
  void shouldDetectEveryDeclaredPattern() {
    var sensorContext = SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME);
    addFiles(sensorContext,
      "skills/my-skill/SKILL.md",
      "CLAUDE.md",
      ".codex/AGENTS.md",
      ".codex/AGENTS.override.md",
      "AGENTS.md",
      "GEMINI.md",
      ".cursor/rules/style.md",
      ".cursorrules",
      ".windsurfrules",
      ".mcp.json",
      ".cursor/mcp.json",
      ".aider.conf.yml",
      ".continue/config.json",
      ".clinerules",
      ".github/copilot-instructions.md",
      ".codex/config.toml",
      "src/Main.java");

    var telemetryReporter = spy(new TelemetryReporter(sensorContext));
    AgenticArtifactsTelemetry.measureAgenticArtifacts(sensorContext, telemetryReporter);

    // skills/my-skill/SKILL.md does not match the more specific .claude/skills/**/SKILL.md pattern,
    // so it falls through to the vendor-neutral pattern.
    verify(telemetryReporter).addNumericMeasure("agentic_skill_files_count", 1);
    verify(telemetryReporter).addNumericMeasure("agentic_instructions_files_count", 8);
    verify(telemetryReporter).addNumericMeasure("agentic_mcp_config_files_count", 2);
    verify(telemetryReporter).addNumericMeasure("agentic_other_agent_files_count", 5);
    verify(telemetryReporter).addListAsStringMeasure("agentic_tools",
      new TreeSet<>(Set.of("vendor-neutral", "claude-code", "gemini", "cursor", "windsurf", "aider", "continue", "cline", "github-copilot",
        "codex")));
  }

  @Test
  void shouldDetectCodexFromNestedInstructionsWithoutConfigToml() {
    var sensorContext = SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME);
    addFiles(sensorContext, "packages/foo/.codex/AGENTS.md");

    var telemetryReporter = spy(new TelemetryReporter(sensorContext));
    AgenticArtifactsTelemetry.measureAgenticArtifacts(sensorContext, telemetryReporter);

    // A nested .codex/AGENTS.md with no config.toml anywhere in the repo must still be detected as
    // codex: the instructions pattern is not anchored to the repo root.
    verify(telemetryReporter).addNumericMeasure("agentic_instructions_files_count", 1);
    verify(telemetryReporter).addListAsStringMeasure("agentic_tools", new TreeSet<>(Set.of("codex")));
  }

  @Test
  void shouldCountOverlappingSkillFileOnceUnderFirstMatchingPattern() {
    var sensorContext = SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME);
    addFiles(sensorContext, ".claude/skills/my-skill/SKILL.md");

    var telemetryReporter = spy(new TelemetryReporter(sensorContext));
    AgenticArtifactsTelemetry.measureAgenticArtifacts(sensorContext, telemetryReporter);

    // .claude/skills/my-skill/SKILL.md matches both the claude-code and vendor-neutral skill
    // patterns; since claude-code is listed first, the file is counted once under claude-code.
    verify(telemetryReporter).addNumericMeasure("agentic_skill_files_count", 1);
    verify(telemetryReporter).addListAsStringMeasure("agentic_tools", new TreeSet<>(Set.of("claude-code")));
  }

  @Test
  void shouldDeduplicateToolsButAccumulateCountAcrossMultipleFiles() {
    var sensorContext = SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME);
    addFiles(sensorContext, "skills/first-skill/SKILL.md", "skills/second-skill/SKILL.md");

    var telemetryReporter = spy(new TelemetryReporter(sensorContext));
    AgenticArtifactsTelemetry.measureAgenticArtifacts(sensorContext, telemetryReporter);

    verify(telemetryReporter).addNumericMeasure("agentic_skill_files_count", 2);
    verify(telemetryReporter).addListAsStringMeasure("agentic_tools", new TreeSet<>(Set.of("vendor-neutral")));
  }

  @Test
  void shouldReportZeroCountsAndNoToolsWhenNoFilesMatch() {
    var sensorContext = SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME);
    addFiles(sensorContext, "src/Main.java", "README.md");

    var telemetryReporter = spy(new TelemetryReporter(sensorContext));
    AgenticArtifactsTelemetry.measureAgenticArtifacts(sensorContext, telemetryReporter);

    verify(telemetryReporter).addNumericMeasure("agentic_skill_files_count", 0);
    verify(telemetryReporter).addNumericMeasure("agentic_instructions_files_count", 0);
    verify(telemetryReporter).addNumericMeasure("agentic_mcp_config_files_count", 0);
    verify(telemetryReporter).addNumericMeasure("agentic_other_agent_files_count", 0);
    verify(telemetryReporter, never()).addListAsStringMeasure(anyString(), anyList());
  }

  @Test
  void shouldNotComputeAnythingInSonarLintContext() {
    var sensorContext = SensorContextTester.create(Path.of(".")).setRuntime(SONARLINT_RUNTIME);
    addFiles(sensorContext, "CLAUDE.md");

    var telemetryReporter = spy(new TelemetryReporter(sensorContext));
    AgenticArtifactsTelemetry.measureAgenticArtifacts(sensorContext, telemetryReporter);

    verifyNoInteractions(telemetryReporter);
  }

  @Test
  void shouldNotComputeAnythingWhenHiddenFilesAnalysisIsNotSupported() {
    var sensorContext = SensorContextTester.create(Path.of(".")).setRuntime(SONARQUBE_RUNTIME_WITHOUT_HIDDEN_FILES_SUPPORT);
    addFiles(sensorContext, "CLAUDE.md");

    var telemetryReporter = spy(new TelemetryReporter(sensorContext));
    AgenticArtifactsTelemetry.measureAgenticArtifacts(sensorContext, telemetryReporter);

    verifyNoInteractions(telemetryReporter);
  }

  private static void addFiles(SensorContextTester sensorContext, String... paths) {
    for (String path : paths) {
      InputFile inputFile = inputFile(Path.of(path), "");
      sensorContext.fileSystem().add(inputFile);
    }
  }
}
