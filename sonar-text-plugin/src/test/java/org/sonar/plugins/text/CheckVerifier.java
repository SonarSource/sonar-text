/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2022 SonarSource SA
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
package org.sonar.plugins.text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.plugins.text.api.TextCheck;
import org.sonar.plugins.text.core.InputFileContext;

import static org.junit.jupiter.api.Assertions.fail;

public class CheckVerifier {

  /**
   * Verify that if <code>check</code> is run on the specified file, exactly the expected {@link LineIssue}s are found.
   * {@link LineIssue#message} can be empty for expected issues. In that case the message is not considered in the comparison.
   * <b>It is currently not possible to test multiple issues on the same line.</b>
   */
  public static void verify(TextCheck check, String testFileRelativePath, LineIssue... expected) throws IOException {
    SensorContextTester context = SensorContextTester.create(Paths.get("src/test/resources"));
    InputFileContext inputFileContext = new InputFileContext(context, inputFile(testFileRelativePath));
    inputFileContext.loadContent();
    check.analyze(inputFileContext);
    verifyLineIssues(context, Arrays.asList(expected));
  }

  private static void verifyLineIssues(SensorContextTester context, List<LineIssue> expected) {
    List<LineIssue> actual = context.allIssues().stream()
            .map(Issue::primaryLocation)
            .map(location -> new LineIssue(location.textRange().start().line(), location.message()))
            .collect(Collectors.toList());

    List<LineIssue> missing = missingBasedOnLine(actual, expected);
    List<LineIssue> unexpected = missingBasedOnLine(expected, actual);

    Map<LineIssue, LineIssue> sameLineButDifferentMessage = new HashMap<>();
    expected.stream()
      .filter(e -> !(e.message == null || missing.contains(e) || actual.contains(e)))
      .forEach(e -> sameLineButDifferentMessage.put(e, actual.stream()
        .filter(a -> (e.lineNumber == a.lineNumber))
        .findFirst().orElseThrow(() -> new IllegalStateException("This should never happen. The issue would be in missing already."))));

    StringBuilder resultSb = new StringBuilder();
    if (!missing.isEmpty()) {
      resultSb.append("Missing Issues:\n");
      missing.forEach(m -> resultSb.append(m).append("\n"));
    }
    if (!unexpected.isEmpty()) {
      resultSb.append("Unexpected Issues:\n");
      unexpected.forEach(u -> resultSb.append(u).append("\n"));
    }
    if (!sameLineButDifferentMessage.isEmpty()) {
      resultSb.append("Unexpected Messages:\n");
      sameLineButDifferentMessage.forEach((a, b) -> resultSb.append(a).append(" -> ").append(b).append("\n"));
    }

    if (resultSb.length() > 0) {
      fail(resultSb.toString());
    }
  }

  /**
   * Returns issues that are in <code>comparedTo</code> but not in <code>from</code> solely based on the line numbers
   */
  private static List<LineIssue> missingBasedOnLine(List<LineIssue> from, List<LineIssue> comparedTo) {
    List<Integer> fromLines = from.stream().map(f -> f.lineNumber).collect(Collectors.toList());
    return comparedTo.stream()
      .filter(e -> !(fromLines.contains(e.lineNumber)))
      .collect(Collectors.toList());
  }

  public static class LineIssue {

    public int lineNumber;
    public String message;

    public LineIssue(int lineNumber, @Nullable String message) {
      this.lineNumber = lineNumber;
      this.message = message;
    }

    public LineIssue(int lineNumber) {
      this(lineNumber, null);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      LineIssue lineIssue = (LineIssue) o;
      return lineNumber == lineIssue.lineNumber && Objects.equals(message, lineIssue.message);
    }

    @Override
    public int hashCode() {
      return Objects.hash(lineNumber, message);
    }

    @Override
    public String toString() {
      return String.format("Line \"%d\" / Message \"%s\"", lineNumber, message);
    }
  }

  private static InputFile inputFile(String testFileRelativePath) {
    Path filePath = Paths.get("src/test/resources/checks/" + testFileRelativePath);
    InputFile result = null;
    try {
      result = TestInputFileBuilder.create("test", testFileRelativePath)
        .setCharset(StandardCharsets.UTF_8)
        .setContents(Files.readString(filePath))
        .build();
    } catch (IOException e) {
      fail("Could not read the file: " + e.getMessage());
    }
    return result;
  }

}
