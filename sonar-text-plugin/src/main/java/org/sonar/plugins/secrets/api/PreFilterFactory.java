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
package org.sonar.plugins.secrets.api;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.configuration.model.matching.filter.FileFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;

public class PreFilterFactory {
  private PreFilterFactory() {}

  private static final Predicate<InputFileContext> INCLUDE_ALL_FILES = ctx -> true;
  private static final Predicate<InputFileContext> REJECT_ALL_FILES = ctx -> false;

  public static Predicate<InputFileContext> createPredicate(@Nullable PreModule pre) {
    if (pre == null) {
      return INCLUDE_ALL_FILES;
    }

    Predicate<InputFileContext> predicate = INCLUDE_ALL_FILES;
    FileFilter include = pre.getInclude();
    FileFilter reject = pre.getReject();
    if (reject != null && include == null) {
      predicate = INCLUDE_ALL_FILES.and(Predicate.not(ctx -> matches(pre.getReject(), ctx)));
    } else if (reject != null) {
      predicate = INCLUDE_ALL_FILES.and(ctx -> matches(include, ctx))
              .and(Predicate.not(ctx -> matches(pre.getReject(), ctx)));
    } else if (include != null) {
      predicate = REJECT_ALL_FILES.or(ctx -> matches(include, ctx));
    }
    return predicate;
  }

  private static boolean matches(FileFilter filter, InputFileContext ctx) {
    return anyMatch(filter.getPaths(), PreFilterFactory::matchesPath, ctx) ||
      anyMatch(filter.getExt(), PreFilterFactory::matchesExt, ctx) ||
      anyMatch(filter.getContent(), PreFilterFactory::matchesContent, ctx);
  }

  private static boolean anyMatch(@Nullable List<String> input, BiPredicate<String, InputFileContext> predicate, InputFileContext ctx) {
    if (input == null) {
      return false;
    }
    return input.stream().anyMatch(s -> predicate.test(s, ctx));
  }

  static boolean matchesPath(String path, InputFileContext ctx) {
    if (path.isBlank()) {
      return false;
    } else if (path.contains("*")) {
      PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/" + path);
      return matcher.matches(Path.of(ctx.getInputFile().uri()));
    } else {
      return ctx.getInputFile().uri().toString().contains(path);
    }
  }

  static boolean matchesContent(String content, InputFileContext ctx) {
    if (content.isBlank()) {
      return false;
    }
    String contentLowerCase = content.toLowerCase(Locale.getDefault());
    return ctx.lines().stream().anyMatch(line -> line.toLowerCase(Locale.getDefault()).contains(contentLowerCase));
  }

  static boolean matchesExt(String ext, InputFileContext ctx) {
    if (ext.isBlank()) {
      return false;
    }
    return ctx.getInputFile().filename().endsWith(ext);
  }
}
