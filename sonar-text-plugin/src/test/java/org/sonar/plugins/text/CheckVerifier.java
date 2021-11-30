/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2021 SonarSource SA
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.plugins.text.api.CheckContext;
import org.sonar.plugins.text.api.CommonCheck;
import org.sonar.plugins.text.api.InitContext;

import static org.junit.jupiter.api.Assertions.fail;

public class CheckVerifier {

  public static void verify(CommonCheck check, String testFileRelativePath, LineIssue... expected) {
    TestContext ctx = new TestContext();
    check.initialize(ctx);
    ctx.consumers.forEach(c -> c.accept(ctx, inputFile(testFileRelativePath)));
    verifyLineIssues(ctx.raisedLineIssues, Arrays.asList(expected));
  }

  private static void verifyLineIssues(List<LineIssue> actual, List<LineIssue> expected) {
    List<LineIssue> missing = expected.stream()
      .filter(e -> actual.stream().noneMatch(a -> a.lineNumber == e.lineNumber))
      .collect(Collectors.toList());

    List<LineIssue> unexpected = actual.stream()
      .filter(a -> expected.stream().noneMatch(e -> e.lineNumber == a.lineNumber))
      .collect(Collectors.toList());

    Map<LineIssue, LineIssue> differentMessage = new HashMap<>();
    expected.stream()
      .filter(e -> !(e.message == null))
      .filter(e -> !(missing.contains(e)))
      .filter(e -> !(actual.contains(e)))
      .forEach(e -> differentMessage.put(e, actual.stream()
        .filter(a -> (e.lineNumber == a.lineNumber))
        .findFirst()
        .get())
      );

    StringBuilder resultSb = new StringBuilder();
    if (!missing.isEmpty()) {
      resultSb.append("Missing Issues:\n");
      missing.forEach(m -> resultSb.append(m).append("\n"));
    }
    if (!unexpected.isEmpty()) {
      resultSb.append("Unexpected Issues:\n");
      unexpected.forEach(u -> resultSb.append(u).append("\n"));
    }
    if (!differentMessage.isEmpty()) {
      resultSb.append("Unexpected Messages:\n");
      differentMessage.forEach((a, b) -> resultSb.append(a).append(" -> ").append(b).append("\n"));
    }

    String result = resultSb.toString();
    if (!result.isEmpty()) {
      fail(result);
    }
  }

  static class TestContext implements InitContext, CheckContext {

    List<BiConsumer<CheckContext, InputFile>> consumers = new ArrayList<>();
    List<LineIssue> raisedLineIssues = new ArrayList<>();

    @Override
    public void reportLineIssue(int line, String message) {
      raisedLineIssues.add(new LineIssue(line, message));
    }

    @Override
    public void register(BiConsumer<CheckContext, InputFile> visitor) {
      consumers.add(visitor);
    }
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
      e.printStackTrace();
    }
    return result;
  }
}
