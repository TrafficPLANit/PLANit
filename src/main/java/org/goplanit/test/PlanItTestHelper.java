package org.goplanit.test;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Helper class used by unit tests
 *
 * @author gman6028, markr
 *
 */
public class PlanItTestHelper {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItTestHelper.class.getCanonicalName());

  /**
   * Compares the contents of two text files
   *
   * In this test the text contents of the files must be exactly equal. This test
   * can be applied to CSV, XML file types
   *
   * @param file1 location of the first file to be compared
   * @param file2 location of the second file to be compared
   * @param printFilesOnFalse when comparison returns false we can print the files when set to true, otherwise not
   * @return true if the contents of the two files are exactly equal, false otherwise
   * @throws IOException thrown if there is an error opening one of the files
   */
  public static boolean compareFilesExact(final String file1, final String file2, final boolean printFilesOnFalse) throws IOException {
    final var charSetName = "utf-8";
    final Path f1 = Path.of(file1).toAbsolutePath();
    if(Files.notExists(f1)){
      LOGGER.warning(String.format("File %s does not exist, printing available xml and csv files in dir",f1));
      FileUtils.listFiles(f1.getParent().toFile(),new String[]{"csv","xml"},false).forEach(f -> LOGGER.warning(f.toString()));
      return false;
    }
    final Path f2 = Path.of(file2).toAbsolutePath();
    if(Files.notExists(f2)){
      LOGGER.warning(String.format("File %s does not exist, printing available xml and csv files in dir",f2));
      FileUtils.listFiles(f2.getParent().toFile(),new String[]{"csv","xml"},false).forEach(f -> LOGGER.warning(f.toString()));
      return false;
    }

    final boolean contentEquals = FileUtils.contentEqualsIgnoreEOL(f1.toFile(), f2.toFile(), charSetName);
    if(!contentEquals && printFilesOnFalse) {
      LOGGER.warning("FILE NOT THE SAME: Printing contents for comparison");
      LOGGER.warning("File 1:");
      LOGGER.warning(FileUtils.readFileToString(f1.toFile(), charSetName));
      LOGGER.warning("File 2:");
      LOGGER.warning(FileUtils.readFileToString(f2.toFile(), charSetName));
    }
    return contentEquals;
  }

}