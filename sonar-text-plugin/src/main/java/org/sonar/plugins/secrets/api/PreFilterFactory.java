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
package org.sonar.plugins.secrets.api;

import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.secrets.configuration.model.Selectivity;
import org.sonar.plugins.secrets.configuration.model.matching.filter.FileFilter;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;

import static java.util.function.Predicate.not;

/**
 * The Factory class for producing a Predicate from {@link PreModule}.
 */
public final class PreFilterFactory {
  private static final Logger LOG = LoggerFactory.getLogger(PreFilterFactory.class);
  private static final Predicate<InputFileContext> NO_LANGUAGE_PREDICATE = ctx -> ctx.getInputFile().language() == null;
  private static final Predicate<InputFileContext> AUTOMATIC_NO_TEST_FILE_FILTER = AutomaticTestFileFilter.isNotAutomaticallyDetectedTestFile();

  public static final Predicate<InputFileContext> INCLUDE_ONLY_MAIN_FILES = ctx -> InputFile.Type.MAIN == ctx.getInputFile().type();

  private PreFilterFactory() {
  }

  /**
   * Produce a predicate from {@link PreModule}.
   *
   * @param pre                            the input {@link PreModule}
   * @param specificationConfiguration     the configuration
   * @param shouldExecuteContentPreFilters whether content pre-filters should be executed or they have been executed earlier
   * @return a predicate
   */
  public static Predicate<InputFileContext> createPredicate(
    @Nullable PreModule pre,
    Selectivity selectivity,
    SpecificationConfiguration specificationConfiguration,
    boolean shouldExecuteContentPreFilters) {
    var predicate = appendSelectivityPredicate(INCLUDE_ONLY_MAIN_FILES, selectivity);
    predicate = appendAutomaticNoTestFileFilter(predicate, specificationConfiguration);

    if (pre == null) {
      return predicate;
    }

    FileFilter include = pre.getInclude();
    FileFilter reject = pre.getReject();
    if (reject != null) {
      predicate = predicate.and(ctx -> notMatches(reject, ctx));
    }
    if (include != null) {
      predicate = predicate.and(ctx -> matches(include, ctx, shouldExecuteContentPreFilters));
    }
    return predicate;
  }

  static Predicate<InputFileContext> appendSelectivityPredicate(Predicate<InputFileContext> predicate, Selectivity selectivity) {
    if (selectivity == Selectivity.ANALYZER_GENERIC) {
      return predicate.and(NO_LANGUAGE_PREDICATE);
    }
    return predicate;
  }

  public static Predicate<InputFileContext> appendAutomaticNoTestFileFilter(Predicate<InputFileContext> predicate, SpecificationConfiguration specificationConfiguration) {
    if (specificationConfiguration.automaticTestFileDetection()) {
      return predicate.and(AUTOMATIC_NO_TEST_FILE_FILTER);
    }
    return predicate;
  }

  private static boolean matches(FileFilter filter, InputFileContext ctx, boolean shouldExecuteContentPreFilters) {
    Predicate<InputFileContext> isPathMatch = c -> filter.getPaths().isEmpty() || anyMatch(filter.getPaths(), PreFilterFactory::matchesPath, c);
    Predicate<InputFileContext> isExtMatch = c -> filter.getExt().isEmpty() || anyMatch(filter.getExt(), PreFilterFactory::matchesExt, c);
    Predicate<InputFileContext> isContentMatch = c -> true;
    if (shouldExecuteContentPreFilters) {
      isContentMatch = c -> filter.getContent().isEmpty() || anyMatch(filter.getContent(), PreFilterFactory::matchesContent, c);
    }
    return isPathMatch
      .and(isExtMatch)
      .and(isContentMatch)
      .test(ctx);
  }

  private static boolean notMatches(FileFilter filter, InputFileContext ctx) {
    Predicate<InputFileContext> isPathMatch = c -> anyMatch(filter.getPaths(), PreFilterFactory::matchesPath, c);
    Predicate<InputFileContext> isExtMatch = c -> anyMatch(filter.getExt(), PreFilterFactory::matchesExt, c);
    Predicate<InputFileContext> isContentMatch = c -> anyMatch(filter.getContent(), PreFilterFactory::matchesContent, c);
    return not(isPathMatch)
      .and(not(isExtMatch))
      .and(not(isContentMatch))
      .test(ctx);
  }

  private static boolean anyMatch(List<String> filterElements, BiPredicate<String, InputFileContext> filterFunction, InputFileContext ctx) {
    return filterElements.stream().anyMatch(filterElement -> filterFunction.test(filterElement, ctx));
  }

  static boolean matchesPath(String path, InputFileContext ctx) {
    if (path.isBlank()) {
      LOG.warn("Parameter <paths> is blank in pre filter, will skip filtering");
      return false;
    } else {
      return ctx.getFileSystem().predicates().matchesPathPattern(path).apply(ctx.getInputFile());
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
