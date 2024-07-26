package org.goplanit.output.formatter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.goplanit.od.path.OdMultiPaths;
import org.goplanit.od.skim.OdSkimMatrix;
import org.goplanit.od.skim.OdSkimMatrix.OdSkimMatrixIterator;
import org.goplanit.output.adapter.*;
import org.goplanit.output.configuration.OutputConfiguration;
import org.goplanit.output.configuration.OutputTypeConfiguration;
import org.goplanit.output.configuration.PathOutputTypeConfiguration;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.OutputTypeEnum;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.output.OutputUtils;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.unit.VehiclesUnit;

import java.io.FileWriter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class containing common methods required by classes which write results to CSV output files
 * 
 * @author gman6028, markr
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
   * Based on the output type configuration generate the CSV header
   * @param outputTypeConfiguration to use
   * @return CSV header as list of strings
   */
  protected static List<String> generateCsvHeader(final OutputTypeConfiguration outputTypeConfiguration){
    return outputTypeConfiguration.getOutputProperties().stream().map(
        OutputProperty::getName).collect(Collectors.toList());
  }

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected CsvFileOutputFormatter(IdGroupingToken groupId) {
    super(groupId);
    this.csvFileNameMap = new HashMap<>();
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
   */
  protected Map<Mode, List<Object>> constructSimulationResultsForCurrentTimePeriod(
      OutputConfiguration outputConfiguration,
      OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType,
      OutputAdapter outputAdapter,
      Set<Mode> modes,
      TimePeriod timePeriod) {

    PlanItRunTimeException.throwIf(!(currentOutputType instanceof OutputType), "currentOutputType not compatible with simulation output");
    var rowValuesByMode = new TreeMap<Mode, List<Object>>();

    OutputType outputType = (OutputType) currentOutputType;

    var simulationOutputTypeAdapter = (SimulationOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);
    SortedSet<OutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();

    for (Mode mode : modes) {
      // ensure that if vehicles are used as the output unit rather than pcu, the correct conversion factor is applied, namely
      // the current mode's conversion factor
      VehiclesUnit.updatePcuToVehicleFactor(1 / mode.getPcu());

      List<Object> rowValues = outputProperties.stream()
          .map(outputProperty -> simulationOutputTypeAdapter.getSimulationOutputPropertyValue(
              outputProperty, mode, timePeriod).orElse(null))
          .map(OutputUtils::formatObject).collect(Collectors.toList());
      rowValuesByMode.put(mode, rowValues);
    }
    return rowValuesByMode;
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
  protected PlanItException writeOdResultsForCurrentTimePeriodToCsvPrinter(
      OutputConfiguration outputConfiguration,
      OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType,
      OutputAdapter outputAdapter,
      Set<Mode> modes,
      TimePeriod timePeriod,
      CSVPrinter csvPrinter) {
    try {
      // main type information
      OdOutputTypeAdapter odOutputTypeAdapter = (OdOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputTypeConfiguration.getOutputType());
      SortedSet<OutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();

      // verify if current suboutput type is compatible with the provided output
      PlanItException.throwIf(!(currentOutputType instanceof OdSkimSubOutputType),
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
          if(!cost.isPresent()) {
            throw new PlanItRunTimeException("Cost could not be retrieved when persisting");
          }

          if (outputConfiguration.isPersistZeroFlow() || cost.get() > Precision.EPSILON_6) {
            csvPrinter.printRecord(
                    outputProperties.stream().map(outputProperty -> odOutputTypeAdapter.getOdOutputPropertyValue(
                            outputProperty, odMatrixIterator, mode, timePeriod).orElse(null)).map(OutputUtils::formatObject));
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

        Optional<OdMultiPaths<?,?>> odPaths = pathOutputTypeAdapter.getOdMultiPaths(mode);
        odPaths.orElseThrow(() -> new PlanItException("od paths could not be retrieved when persisting"));

        for (var odMultiPathIterator = odPaths.get().iterator(); odMultiPathIterator.hasNext();) {
          var odMultiPaths = odMultiPathIterator.next();

          if (!outputConfiguration.isPersistZeroFlow() && (odMultiPaths == null || odMultiPaths.isEmpty())) {
            continue;
          }

          // skip intra-zonal paths results
          if(odMultiPathIterator.getCurrentOrigin().equals(odMultiPathIterator.getCurrentDestination())){
            continue;
          }

          /* obtain the col vectors for each property, where the number of eventual rows is the # entries in each col vector
           * which is the same for each property, namely how many paths there are for the given OD  */
          List<? extends List<?>> colVectorValues = outputProperties.stream()
              .map(outputProperty -> pathOutputTypeAdapter
                  .getPathOutputPropertyValue(
                          outputProperty, odMultiPathIterator, mode, timePeriod, pathOutputTypeConfiguration.getPathIdentificationType()).orElse(null))
              .collect(Collectors.toList());

          /* transpose the column vectors, e.g., [[1,2,3,4],["a","b","c","d"]] -> [[1,"a"],[2,"b"],[3,"c"],[4,"d"]], and format each value
           * the latter being the actual rows to persist */
          var rowsValues =
              ListUtils.transpose((List<? extends List<Object>>) colVectorValues, OutputUtils::formatObject);

          // now persist per row
          for(var rowValue : rowsValues){
            csvPrinter.printRecord(rowValue);
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
                csvPrinter.printRecord(outputProperties.stream().map(outputProperty ->
                                linkOutputTypeAdapter.getLinkSegmentOutputPropertyValue(
                                        outputProperty, linkSegment, mode, timePeriod).orElse(null)).map(OutputUtils::formatObject));
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
   * @param csvFileName             the name of the CSV output file
   * @return the CSVPrinter object (output values will be written to this in subsequent rows)
   * @throws Exception thrown if there is an error opening the file
   */
  protected CSVPrinter createCsvPrinter(String csvFileName) throws Exception {

    CSVPrinter csvPrinter =
        new CSVPrinter(
            new FileWriter(csvFileName),
            CSVFormat.Builder.create(CSVFormat.DEFAULT).setIgnoreSurroundingSpaces(true).build());

    return csvPrinter;
  }

  /**
   * Add a new name of the CSV output file for a specified output type
   * 
   * @param currentOutputType the specified output type
   * @param csvFileName       the name of the output file to be added
   */
  public void addCsvFileNamePerOutputType(OutputTypeEnum currentOutputType, String csvFileName) {
    if (!csvFileNameMap.containsKey(currentOutputType)) {
      csvFileNameMap.put(currentOutputType, new ArrayList<>());
    }
    csvFileNameMap.get(currentOutputType).add(csvFileName);
  }

}