package org.goplanit.output.formatter;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.goplanit.od.path.OdPathMatrix;
import org.goplanit.od.path.OdPathMatrix.OdPathMatrixIterator;
import org.goplanit.od.skim.OdSkimMatrix;
import org.goplanit.od.skim.OdSkimMatrix.OdSkimMatrixIterator;
import org.goplanit.output.adapter.MacroscopicLinkOutputTypeAdapter;
import org.goplanit.output.adapter.OdOutputTypeAdapter;
import org.goplanit.output.adapter.OutputAdapter;
import org.goplanit.output.adapter.PathOutputTypeAdapter;
import org.goplanit.output.configuration.OutputConfiguration;
import org.goplanit.output.configuration.OutputTypeConfiguration;
import org.goplanit.output.configuration.PathOutputTypeConfiguration;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.OutputTypeEnum;
import org.goplanit.output.enums.SubOutputTypeEnum;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.output.OutputUtils;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.unit.VehiclesUnit;

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
  @SuppressWarnings("unchecked")
  protected PlanItException writeOdResultsForCurrentTimePeriodToCsvPrinter(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, CSVPrinter csvPrinter) {
    try {
      // main type information
      OdOutputTypeAdapter odOutputTypeAdapter = (OdOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputTypeConfiguration.getOutputType());
      SortedSet<OutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();

      // verify if current suboutput type is compatible with the provided output
      PlanItException.throwIf(!(currentOutputType instanceof SubOutputTypeEnum && ((SubOutputTypeEnum) currentOutputType) instanceof OdSkimSubOutputType),
          "currentOutputType is not compatible with od results");

      // sub-type information
      OdSkimSubOutputType currentSubOutputType = (OdSkimSubOutputType) currentOutputType;

      // perform actual persistence
      final OutputProperty odCostProperty = OutputProperty.of(OutputPropertyType.OD_COST);
      for (Mode mode : modes) {

        // ensure that if vehicles are used as the output unit rather than pcu, the correct conversion factor is applied, namely
        // the current mode's conversion factor
        VehiclesUnit.updatePcuToVehicleFactor(1 / mode.getPcu());

        Optional<OdSkimMatrix> odSkimMatrix = odOutputTypeAdapter.getOdSkimMatrix(currentSubOutputType, mode);
        odSkimMatrix.orElseThrow(() -> new PlanItException("od skim matrix could not be retrieved when persisting"));

        for (OdSkimMatrixIterator odMatrixIterator = odSkimMatrix.get().iterator(); odMatrixIterator.hasNext();) {
          odMatrixIterator.next();
          Optional<Double> cost = (Optional<Double>) odOutputTypeAdapter.getOdOutputPropertyValue(odCostProperty, odMatrixIterator, mode, timePeriod);
          cost.orElseThrow(() -> new PlanItException("cost could not be retrieved when persisting"));

          if (outputConfiguration.isPersistZeroFlow() || cost.get() > Precision.EPSILON_6) {
            List<Object> rowValues = outputProperties.stream()
                .map(outputProperty -> odOutputTypeAdapter.getOdOutputPropertyValue(outputProperty, odMatrixIterator, mode, timePeriod).get())
                .map(outValue -> OutputUtils.formatObject(outValue)).collect(Collectors.toList());
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
      SortedSet<OutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();
      for (Mode mode : modes) {
        // ensure that if vehicles are used as the output unit rather than pcu, the correct conversion factor is applied, namely
        // the current mode's conversion factor
        VehiclesUnit.updatePcuToVehicleFactor(1 / mode.getPcu());

        Optional<OdPathMatrix> odPathMatrix = pathOutputTypeAdapter.getOdPathMatrix(mode);
        odPathMatrix.orElseThrow(() -> new PlanItException("od path matrix could not be retrieved when persisting"));

        for (OdPathMatrixIterator odPathIterator = odPathMatrix.get().iterator(); odPathIterator.hasNext();) {
          odPathIterator.next();
          if (outputConfiguration.isPersistZeroFlow() || (odPathIterator.getCurrentValue() != null)) {
            List<Object> rowValues = outputProperties.stream()
                .map(outputProperty -> pathOutputTypeAdapter
                    .getPathOutputPropertyValue(outputProperty, odPathIterator, mode, timePeriod, pathOutputTypeConfiguration.getPathIdentificationType()).get())
                .map(outValue -> OutputUtils.formatObject(outValue)).collect(Collectors.toList());
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

      MacroscopicLinkOutputTypeAdapter linkOutputTypeAdapter = (MacroscopicLinkOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);

      SortedSet<OutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();
      for (Mode mode : modes) {
        // ensure that if vehicles are used as the output unit rather than pcu, the correct conversion factor is applied, namely
        // the current mode's conversion factor
        VehiclesUnit.updatePcuToVehicleFactor(1 / mode.getPcu());

        Optional<Long> networkLayerId = linkOutputTypeAdapter.getInfrastructureLayerIdForMode(mode);
        if (networkLayerId.isPresent()) {
          for (MacroscopicLinkSegment linkSegment : linkOutputTypeAdapter.getPhysicalLinkSegments(networkLayerId.get())) {

            if (linkSegment.isModeAllowed(mode)) {
              Optional<Boolean> flowPositive = linkOutputTypeAdapter.isFlowPositive(linkSegment, mode);
              flowPositive.orElseThrow(() -> new PlanItException("unable to determine if flow is positive on link segment"));

              if (outputConfiguration.isPersistZeroFlow() || flowPositive.get()) {
                List<Object> rowValues = outputProperties.stream()
                    .map(outputProperty -> linkOutputTypeAdapter.getLinkSegmentOutputPropertyValue(outputProperty, linkSegment, mode, timePeriod).get())
                    .map(outValue -> OutputUtils.formatObject(outValue)).collect(Collectors.toList());
                csvPrinter.printRecord(rowValues);
              }
            }
          }
        } else {
          LOGGER.severe(String.format("network layer could not be identified for mode %s by csv output formatter", mode.getXmlId()));
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
    CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(csvFileName), CSVFormat.DEFAULT.withIgnoreSurroundingSpaces());
    List<String> headerValues = outputTypeConfiguration.getOutputProperties().stream().map(OutputProperty::getName).collect(Collectors.toList());
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