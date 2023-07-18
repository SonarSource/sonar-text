package org.sonar.plugins.secrets.api;

import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HeuristicsTest {
    @ParameterizedTest
    @CsvSource({
            "/home/user, true",
            "/home/username, false",
            "C:\\Users\\User, true",
            "asecretstr/ng, false"
    })
    void shouldDeterminePath(String input, boolean isPath) {
        Assertions.assertThat(Heuristics.isPath(input)).isEqualTo(isPath);
    }

    @ParameterizedTest
    @CsvSource({
            "https://sonarsource.com, true",
            "nonsense://secretstring, false",
    })
    void shouldDetermineUri(String input, boolean isUri) {
        Assertions.assertThat(Heuristics.isUri(input)).isEqualTo(isUri);
    }


    @ParameterizedTest
    @CsvSource({
            "path, https://sonarsource.com, false",
            "path;uri, https://sonarsource.com, true",
            "uri, https://sonarsource.com, true",
            "path, /home/user, true",
            "path;uri, /home/user, true",
            "uri, /home/user, false",
    })
    void shouldPerformChecksFromList(String heuristics, String input, boolean shouldMatch) {
        Assertions.assertThat(
                Heuristics.matchesHeuristics(input, Arrays.asList(heuristics.split(";")))
        ).isEqualTo(shouldMatch);
    }
}
