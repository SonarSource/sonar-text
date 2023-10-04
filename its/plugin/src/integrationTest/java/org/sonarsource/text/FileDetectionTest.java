package org.sonarsource.text;

import com.sonar.orchestrator.build.SonarScanner;
import javax.annotation.Nullable;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FileDetectionTest extends TestBase {
  private static final String BASE_DIRECTORY = "projects/file-detection/";
  private static final String NO_SONAR_PROFILE_NAME = "nosonar-profile";
  private static final String PROJECT_KEY = "fileDetection";
  private static int counter = 1;

  @Test
  public void shouldFindAllFilesWithOptionAllFiles() {
    checkQuantityFileAnalyzedForConfiguration("text-files-only", true, null, 2);
  }

  @Test
  public void shouldFindOnlySpecifiedExtension() {
    checkQuantityFileAnalyzedForConfiguration("text-files-only", false, "txt", 1);
  }

  @Test
  public void shouldFindNoFileWhenNoExtensionSpecified() {
    checkQuantityFileAnalyzedForConfiguration("text-files-only", false, null, 0);
  }

  @Test
  public void shouldFindMultipleSpecifiedExtension() {
    checkQuantityFileAnalyzedForConfiguration("text-files-only", false, "bash,txt", 2);
  }

  @Test
  public void shouldFindOnlyTextFilesWithOptionAllFiles() {
    checkQuantityFileAnalyzedForConfiguration("mix-text-binary-files", true, null, 2);
  }

  @Test
  public void shouldFindOnlyTextFileSpecifiedExtension() {
    checkQuantityFileAnalyzedForConfiguration("mix-text-binary-files", false, "txt", 1);
  }

  @Test
  public void shouldFindOnlyBinaryFileWithIssueInSpecifiedExtension() {
    checkQuantityFileAnalyzedForConfiguration( "mix-text-binary-files", false, "exe", 1);
  }

  @Test
  public void shouldFindNoBinaryOrTextFileWhenNoExtensionSpecified() {
    checkQuantityFileAnalyzedForConfiguration("mix-text-binary-files", false, null, 0);
  }

  @Test
  public void shouldFindMultipleTextFileSpecifiedExtension() {
    checkQuantityFileAnalyzedForConfiguration("mix-text-binary-files", false, "bash,txt", 2);
  }

  void checkQuantityFileAnalyzedForConfiguration(String subfolder, boolean analyzeAll, @Nullable String extensions, int quantityFileAnalyzed) {
    // need to define a different project key for every check
    String projectKey = PROJECT_KEY + counter++;
    SonarScanner sonarScanner = getSonarScanner(projectKey, BASE_DIRECTORY + subfolder, NO_SONAR_PROFILE_NAME);
    if (analyzeAll) {
      sonarScanner.setProperty("sonar.text.analyzeAllFiles", "true");
    }
    if (extensions != null) {
      sonarScanner.setProperty("sonar.text.include.file.suffixes", extensions);
    }

    ORCHESTRATOR.executeBuild(sonarScanner);
    if (quantityFileAnalyzed > 0) {
      assertThat(getMeasureAsInt(projectKey, "files")).isEqualTo(quantityFileAnalyzed);
    } else {
      assertThat(getMeasureAsInt(projectKey, "files")).isNull();
    }
  }
}
