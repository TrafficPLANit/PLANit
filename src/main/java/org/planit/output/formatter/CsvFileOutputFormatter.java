package org.planit.output.formatter;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.od.odpath.ODPathIterator;
import org.planit.od.odpath.ODPathMatrix;
import org.planit.output.adapter.LinkOutputTypeAdapter;
import org.planit.output.adapter.ODOutputTypeAdapter;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.adapter.PathOutputTypeAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.configuration.PathOutputTypeConfiguration;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.OutputTypeEnum;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.output.OutputUtils;

/**
 * Class containing common methods required by classes which write results to CSV output files
 * 
 * @author gman6028
 *
 */
public abstract class CsvFileOutputFormatter extends FileOutputFormatter {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(CsvFileOutputFormatter.class.getCanonicalName());

  /**
   * Map of list of CSV output file names for each OutputType
   */
  protected final Map<OutputTypeEnum, List<String>> csvFileNameMap;

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected CsvFileOutputFormatter(IdGroupingToken groupId) {
    super(groupId);
    this.csvFileNameMap = new HashMap<OutputTypeEnum, List<String>>();
  }

  /**
   * Write output values to the OD CSV file for the current iteration
   * 
   * @param outputConfiguration     output configuration
   * @param outputTypeConfiguration output type configuration for the current output type
   * @param currentOutputType       the output type
   * @param outputAdapter           output adapter for the current output type
   * @param modes                   Set of modes for the current assignment
   * @param timePeriod              the current time period
   * @param csvPrinter              CSVPrinter object to record results for this iteration
   * @return PlanItException thrown if the CSV file cannot be created or written to
   */
  protected PlanItException writeOdResultsForCurrentTimePeriodToCsvPrinter(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, CSVPrinter csvPrinter) {
    try {
      // main type information
      ODOutputTypeAdapter odOutputTypeAdapter = (ODOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputTypeConfiguration.getOutputType());
      SortedSet<BaseOutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();

      // verify if current suboutput type is compatible with the provided output
      PlanItException.throwIf(!(currentOutputType instanceof SubOutputTypeEnum && ((SubOutputTypeEnum) currentOutputType) instanceof ODSkimSubOutputType),
          "currentOutputType is not compatible with od results");

      // sub-type information
      ODSkimSubOutputType currentSubOutputType = (ODSkimSubOutputType) currentOutputType;

      // perform actual persistence
      for (Mode mode : modes) {
        ODSkimMatrix odSkimMatrix = odOutputTypeAdapter.getODSkimMatrix(currentSubOutputType, mode);
        for (ODMatrixIterator odMatrixIterator = odSkimMatrix.iterator(); odMatrixIterator.hasNext();) {
          odMatrixIterator.next();
          if (outputConfiguration.isPersistZeroFlow()
              || ((Double) odOutputTypeAdapter.getODOutputPropertyValue(OutputProperty.OD_COST, odMatrixIterator, mode, timePeriod, outputTimeUnit.getMultiplier())) > 0.0) {
            List<Object> rowValues = outputProperties.stream().map(outputProperty -> odOutputTypeAdapter.getODOutputPropertyValue(outputProperty.getOutputProperty(),
                odMatrixIterator, mode, timePeriod, outputTimeUnit.getMultiplier())).map(outValue -> OutputUtils.formatObject(outValue)).collect(Collectors.toList());
            csvPrinter.printRecord(rowValues);
          }
        }
      }
    } catch (PlanItException e) {
      return e;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      return new PlanItException("Error when writing od results for current time period in CSVOutputFileformatter", e);
    }
    return null;
  }

  /**
   * Write output values to the Path CSV file for the current iteration
   * 
   * @param outputConfiguration     output configuration
   * @param outputTypeConfiguration output type configuration for the current output type
   * @param currentOutputType       the output type
   * @param outputAdapter           output adapter for the current output type
   * @param modes                   Set of modes for the current assignment
   * @param timePeriod              the current time period
   * @param csvPrinter              CSVPrinter object to record results for this iteration
   * @return PlanItException thrown if the CSV file cannot be created or written to
   */
  protected PlanItException writePathResultsForCurrentTimePeriodToCsvPrinter(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, CSVPrinter csvPrinter) {
    try {
      PlanItException.throwIf(!(currentOutputType instanceof OutputType), "currentOutputType not compatible with path output");

      OutputType outputType = (OutputType) currentOutputType;
      PathOutputTypeAdapter pathOutputTypeAdapter = (PathOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);
      PathOutputTypeConfiguration pathOutputTypeConfiguration = (PathOutputTypeConfiguration) outputTypeConfiguration;
      SortedSet<BaseOutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();
      for (Mode mode : modes) {
        ODPathMatrix odPathMatrix = pathOutputTypeAdapter.getODPathMatrix(mode);
        for (ODPathIterator odPathIterator = odPathMatrix.iterator(); odPathIterator.hasNext();) {
          odPathIterator.next();
          if (outputConfiguration.isPersistZeroFlow() || (odPathIterator.getCurrentValue() != null)) {            
            List<Object> rowValues = outputProperties.stream().map(outputProperty -> pathOutputTypeAdapter.getPathOutputPropertyValue(outputProperty.getOutputProperty(),
                odPathIterator, mode, timePeriod, pathOutputTypeConfiguration.getPathIdentificationType())).map(outValue -> OutputUtils.formatObject(outValue))
                .collect(Collectors.toList());
            csvPrinter.printRecord(rowValues);
          }
        }
      }
    } catch (PlanItException e) {
      return e;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      return new PlanItException("Error when writing path results for current time period in CSVOutputFileformatter", e);
    }
    return null;
  }

  /**
   * Write output values to the Link CSV file for the current iteration
   * 
   * @param outputConfiguration     output configuration
   * @param outputTypeConfiguration the current output type configuration
   * @param currentOutputType       the output type
   * @param outputAdapter           output adapter for the current output type
   * @param modes                   Set of modes for the current assignment
   * @param timePeriod              the current time period
   * @param csvPrinter              CSVPrinter object to record results for this iteration
   * @return PlanItException thrown if the CSV file cannot be created or written to
   */
  protected PlanItException writeLinkResultsForCurrentTimePeriodToCsvPrinter(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, CSVPrinter csvPrinter) {
    try {
      PlanItException.throwIf(!(currentOutputType instanceof OutputType), "currentOutputType not compatible with link output");

      OutputType outputType = (OutputType) currentOutputType;
      LinkOutputTypeAdapter linkOutputTypeAdapter = (LinkOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);
      SortedSet<BaseOutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();
      for (Mode mode : modes) {
        for (LinkSegment linkSegment : linkOutputTypeAdapter.getPhysicalLinkSegments()) {
          if (outputConfiguration.isPersistZeroFlow() || linkOutputTypeAdapter.isFlowPositive(linkSegment, mode)) {
            List<Object> rowValues = outputProperties.stream().map(outputProperty -> linkOutputTypeAdapter.getLinkOutputPropertyValue(outputProperty.getOutputProperty(),
                linkSegment, mode, timePeriod, outputTimeUnit.getMultiplier())).map(outValue -> OutputUtils.formatObject(outValue)).collect(Collectors.toList());
            csvPrinter.printRecord(rowValues);
          }
        }
      }
    } catch (PlanItException e) {
      return e;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      return new PlanItException("Error when writing link results for current time period in CSVOutputFileformatter", e);
    }
    return null;
  }

  /**
   * Open the CSV output file and write the headers to it
   * 
   * @param outputTypeConfiguration the current output type configuration
   * @param csvFileName             the name of the CSV output file
   * @return the CSVPrinter object (output values will be written to this in subsequent rows)
   * @throws Exception thrown if there is an error opening the file
   */
  protected CSVPrinter openCsvFileAndWriteHeaders(OutputTypeConfiguration outputTypeConfiguration, String csvFileName) throws Exception {
    CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(csvFileName), CSVFormat.DEFAULT);
    List<String> headerValues = outputTypeConfiguration.getOutputProperties().stream().map(BaseOutputProperty::getName).collect(Collectors.toList());
    csvPrinter.printRecord(headerValues);
    return csvPrinter;
  }

  /**
   * Add a new name of the CSV output file for a specified output type
   * 
   * @param currentoutputType the specified output type
   * @param csvFileName       the name of the output file to be added
   */
  public void addCsvFileNamePerOutputType(OutputTypeEnum currentoutputType, String csvFileName) {
    if (!csvFileNameMap.containsKey(currentoutputType)) {
      csvFileNameMap.put(currentoutputType, new ArrayList<String>());
    }
    csvFileNameMap.get(currentoutputType).add(csvFileName);
  }

}