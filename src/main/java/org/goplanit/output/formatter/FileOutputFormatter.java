package org.goplanit.output.formatter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.goplanit.utils.time.TimePeriod;
import org.goplanit.output.enums.OutputType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;

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
   * Generates the name of an output file. All output files have no spaces in them.
   * 
   * @param outputDirectory location output files are to be written
   * @param nameRoot        root name of the output files
   * @param nameExtension   extension of the output files
   * @param timePeriod      the time period
   * @param outputType      the OutputType of the output
   * @param runId           the id of the traffic assignment run
   * @param iteration       current iteration
   * @return the name of the output file in absolute form
   * @throws PlanItException thrown if the output directory cannot be opened
   */
  protected String generateAbsoluteOutputFileName(String outputDirectory, String nameRoot, String nameExtension, TimePeriod timePeriod, OutputType outputType, long runId, int iteration)
      throws PlanItException {
    try {
      Path outputDirPath = Path.of(outputDirectory).toAbsolutePath();
      if (!Files.isDirectory(outputDirPath)) {
        Files.createDirectories(outputDirPath);
      }

      // make sure all spaces are removed from result file
      String nameRootNoSpace = nameRoot.replaceAll(" ", "_");

      String newFileName = null;
      if (timePeriod == null) {
        if (iteration == -1) {
          newFileName = Path.of(outputDirPath.toString(), outputType.value() + "_RunId_" + runId + "_" + nameRootNoSpace + nameExtension).toString();
        } else {
          newFileName = Path.of(outputDirPath.toString(),  outputType.value() + "_RunId_" + runId + "_" + nameRootNoSpace + "_" + iteration + nameExtension).toString();
        }
      } else {
        if (iteration == -1) {
          newFileName = Path.of(outputDirPath.toString(),outputType.value() + "_RunId_" + runId + "_" + nameRootNoSpace + "_" + timePeriod.getDescription().replace(' ', '_')
              + nameExtension).toString();
        } else {
          newFileName =Path.of(outputDirPath.toString(), outputType.value() + "_RunId_" + runId + "_" + nameRootNoSpace + "_" + timePeriod.getDescription().replace(' ', '_') + "_"
              + iteration + nameExtension).toString();
        }
      }
      return newFileName;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when generating output file name in FileOutputFormatter", e);
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
   * @throws PlanItException thrown if the output directory cannot be opened
   */
  protected String generateAbsoluteOutputFileName(String outputDirectory, String nameRoot, String nameExtension, TimePeriod timePeriod, OutputType outputType, long runId)
      throws PlanItException {
    return generateAbsoluteOutputFileName(outputDirectory, nameRoot, nameExtension, timePeriod, outputType, runId, -1);
  }

}