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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class EntropyCheckerTest {

  @Test
  void computeSampleValue() {
    assertThat(EntropyChecker.calculateShannonEntropy("0040878d3579659158d09ad09b6a9849d18e0e22")).isEqualTo(3.587326145256008);
  }

  @Test
  void emptyValue() {
    assertThat(EntropyChecker.calculateShannonEntropy("")).isEqualTo(0.0);
  }

  @Test
  void entropyCheckPositiveDefaultThreshold() {
    assertThat(EntropyChecker.hasLowEntropyWithDefaultThreshold("06c6d5715a1ede6c51fc39ff67fd647f740b656d")).isTrue();
  }

  @Test
  void entropyCheckNegativeLowThreshold() {
    assertThat(EntropyChecker.hasLowEntropy("06c6d5715a1ede6c51fc39ff67fd647f740b656d", 3f)).isFalse();
  }

  @Test
  void entropyCheckNegativeDefaultThreshold() {
    assertThat(EntropyChecker.hasLowEntropyWithDefaultThreshold("qAhEMdXy/MPwEuDlhh7O0AFBuzGvNy7AxpL3sX3q")).isFalse();
  }

  @Test
  void entropyCheckPositiveHighThreshold() {
    assertThat(EntropyChecker.hasLowEntropy("qAhEMdXy/MPwEuDlhh7O0AFBuzGvNy7AxpL3sX3q", 10f)).isTrue();
  }

  @Test
  void thresholdSplitsFalsePositiveGoodEnough() throws IOException {
    double falsePositivesAboveThreshold = processFile("src/test/resources/EntropyChecker/false-positives.txt",
      EntropyChecker.DEFAULT_ENTROPY_THRESHOLD);
    double truePositivesAboveThreshold = processFile("src/test/resources/EntropyChecker/true-positives.txt",
      EntropyChecker.DEFAULT_ENTROPY_THRESHOLD);

    // this assertions can be changed if we will get more data that, for example, will show more false positives
    // the goal of the test to fail if threshold value will be changed, since current value is the sweet spot on data we have so far
    assertThat(falsePositivesAboveThreshold).isLessThan(0.5);
    assertThat(truePositivesAboveThreshold).isEqualTo(100.0);
  }

  private double processFile(String fileName, double threshold) throws IOException {
    File file = new File(fileName);
    int aboveThreshold = 0;
    int total = 0;
    try (FileReader fileReader = new FileReader(file)) {
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        total++;
        double entropy = EntropyChecker.calculateShannonEntropy(line);
        if (entropy > threshold) {
          aboveThreshold++;
        }
      }
    }
    return 1.0 * aboveThreshold / total * 100;
  }
}
