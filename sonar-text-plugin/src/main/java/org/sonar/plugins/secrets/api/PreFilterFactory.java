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

import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.configuration.model.matching.filter.FileFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;

public class PreFilterFactory {
  private PreFilterFactory() {
  }

  private static final Logger LOG = Loggers.get(PreFilterFactory.class);
  private static final Predicate<InputFileContext> INCLUDE_ALL_FILES = ctx -> true;

  public static Predicate<InputFileContext> createPredicate(@Nullable PreModule pre) {
    if (pre == null) {
      return INCLUDE_ALL_FILES;
    }

    Predicate<InputFileContext> predicate = INCLUDE_ALL_FILES;
    FileFilter include = pre.getInclude();
    FileFilter reject = pre.getReject();
    if (reject != null) {
      predicate = predicate.and(Predicate.not(ctx -> matches(reject, ctx)));
    }
    if (include != null) {
      predicate = predicate.and(ctx -> matches(include, ctx));
    }
    return predicate;
  }

  private static boolean matches(FileFilter filter, InputFileContext ctx) {
    return anyMatch(filter.getPaths(), PreFilterFactory::matchesPath, ctx) ||
      anyMatch(filter.getExt(), PreFilterFactory::matchesExt, ctx) ||
      anyMatch(filter.getContent(), PreFilterFactory::matchesContent, ctx);
  }

  private static boolean anyMatch(@Nullable List<String> filterElements, BiPredicate<String, InputFileContext> filterFunction, InputFileContext ctx) {
    if (filterElements == null) {
      return false;
    }
    return filterElements.stream().anyMatch(filterElement -> filterFunction.test(filterElement, ctx));
  }

  static boolean matchesPath(String path, InputFileContext ctx) {
    if (path.isBlank()) {
      LOG.warn("Parameter <paths> is blank in pre filter, will skip filtering");
      return false;
    } else if (path.contains("*")) {
      return ctx.getFileSystem().predicates().matchesPathPattern(path).apply(ctx.getInputFile());
    } else {
      return ctx.getFileSystem().predicates().hasRelativePath(path).apply(ctx.getInputFile());
    }
  }

  static boolean matchesContent(String content, InputFileContext ctx) {
    if (content.isBlank()) {
      LOG.warn("Parameter <content> is blank in pre filter, will skip filtering");
      return false;
    }
    String contentLowerCase = content.toLowerCase(Locale.getDefault());
    return ctx.lines().stream().anyMatch(line -> line.toLowerCase(Locale.getDefault()).contains(contentLowerCase));
  }

  static boolean matchesExt(String ext, InputFileContext ctx) {
    if (ext.isBlank()) {
      LOG.warn("Parameter <ext> is blank in pre filter, will skip filtering");
      return false;
    }
    String extWithoutDot;
    if (ext.startsWith(".")) {
      extWithoutDot = ext.substring(ext.lastIndexOf(".") + 1);
    } else {
      extWithoutDot = ext;
    }
    return ctx.getFileSystem().predicates().hasExtension(extWithoutDot).apply(ctx.getInputFile());
  }
}
