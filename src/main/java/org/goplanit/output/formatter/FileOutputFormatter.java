package org.goplanit.output.formatter;

import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.time.TimePeriod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Common methods used by output formatters which write data to physical files
 * 
 * @author gman6028
 *
 */
public abstract class FileOutputFormatter extends BaseOutputFormatter {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(FileOutputFormatter.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected FileOutputFormatter(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Generates the name of an output file. All output files have no spaces in them. In case one or more directories
   * do not exist, create themm
   *
   * @param outputDirectory location output files are to be written
   * @param nameRoot        root name of the output files
   * @param nameExtension   extension of the output files
   * @param timePeriod      the time period
   * @param outputType      the OutputType of the output
   * @param runId           the id of the traffic assignment run
   * @param iteration       current iteration
   * @return the name of the output file in absolute form
   */
  protected static String generateAbsoluteCsvFileName(
      String outputDirectory,
      String nameRoot,
      String nameExtension,
      TimePeriod timePeriod,
      OutputType outputType,
      long runId,
      int iteration){

    return generateAbsoluteCsvFileName(
        outputDirectory, nameRoot, nameExtension, timePeriod, outputType, runId, iteration, "");
  }

  /**
   * Generates the name of an output file. All output files have no spaces in them. In case one or more directories
   * do not exist, create themm
   *
   * @param outputDirectory location output files are to be written
   * @param nameRoot        root name of the output files
   * @param nameExtension   extension of the output files
   * @param timePeriod      the time period
   * @param outputType      the OutputType of the output
   * @param runId           the id of the traffic assignment run
   * @param iteration       current iteration
   * @param customInject    custom inject into file name to customise as required
   * @return the name of the output file in absolute form
   */
  protected static String generateAbsoluteCsvFileName(
      String outputDirectory,
      String nameRoot,
      String nameExtension,
      TimePeriod timePeriod,
      OutputType outputType,
      long runId,
      int iteration,
      String customInject){

    try {
      Path outputDirPath = Path.of(outputDirectory).toAbsolutePath();
      if (!Files.isDirectory(outputDirPath)) {
        Files.createDirectories(outputDirPath);
      }

      String timePeriodStr = "";
      if (timePeriod != null) {
        timePeriodStr = "_" + timePeriod.getDescription().replace(' ', '_');
      }

      String iterationStr = "";
      if (iteration != -1) {
        iterationStr = "_" + iteration;
      }

      String fileNameWithExtension =
          String.format(
              "%s_RunId_%d_%s%s%s%s%s",
              outputType.value(),
              runId,
              nameRoot.replaceAll(" ", "_"),
              customInject,
              timePeriodStr,
              iterationStr,
              nameExtension);
      String newFileName =Path.of(
          outputDirPath.toString(),fileNameWithExtension).toString();

      return newFileName;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException("Error when generating output file name in FileOutputFormatter", e);
    }
  }

  /**
   * Generates the name of an output file.
   * 
   * @param outputDirectory location output files are to be written
   * @param nameRoot        root name of the output files
   * @param nameExtension   extension of the output files
   * @param timePeriod      timePeriod
   * @param outputType      the OutputType of the output
   * @param runId           the id of the traffic assignment run
   * @return the name of the output file
   */
  protected String generateAbsoluteCsvFileName(
      String outputDirectory,
      String nameRoot,
      String nameExtension,
      TimePeriod timePeriod,
      OutputType outputType,
      long runId){
    return generateAbsoluteCsvFileName(
        outputDirectory, nameRoot, nameExtension, timePeriod, outputType, runId, -1);
  }

}