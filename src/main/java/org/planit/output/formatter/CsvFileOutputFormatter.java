package org.planit.output.formatter;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.od.odpath.ODPath;
import org.planit.od.odpath.ODPathIterator;
import org.planit.output.adapter.LinkOutputTypeAdapter;
import org.planit.output.adapter.ODOutputTypeAdapter;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.adapter.ODPathOutputTypeAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.ODSkimOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.property.BaseOutputProperty;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.utils.OutputUtils;

/**
 * Class containing common methods required by classes which write results to CSV output files
 * 
 * @author gman6028
 *
 */
public abstract class CsvFileOutputFormatter extends FileOutputFormatter {

	/**
	 * Map of list of CSV output file names for each OutputType
	 */
	protected Map<OutputType, List<String>> csvFileNameMap;

	/**
	 * Write output values to the OD CSV file for the current iteration
	 * 
	 * @param outputTypeConfiguration output type configuration for the current output type
	 * @param outputAdapter output adapter for the current output type
	 * @param modes         Set of modes for the current assignment
	 * @param timePeriod    the current time period
	 * @param csvPrinter    CSVPrinter object to record results for this iteration
	 * @return PlanItException thrown if the CSV file cannot be created or written
	 *         to
	 */
	protected PlanItException writeOdResultsForCurrentTimePeriodToCsvPrinter(	OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, CSVPrinter csvPrinter) {
		try {
			ODOutputTypeAdapter odOutputTypeAdapter = (ODOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputTypeConfiguration.getOutputType());
			SortedSet<BaseOutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();
			for (ODSkimOutputType odSkimOutputType : outputTypeConfiguration.getActiveOdSkimOutputTypes()) {
				for (Mode mode : modes) {
					ODSkimMatrix odSkimMatrix = odOutputTypeAdapter.getODSkimMatrix(odSkimOutputType, mode);
					for (ODMatrixIterator odMatrixIterator = odSkimMatrix.iterator(); odMatrixIterator.hasNext();) {
						odMatrixIterator.next();
						List<Object> rowValues = outputProperties.stream()
								.map(outputProperty -> odOutputTypeAdapter.getODOutputPropertyValue(outputProperty.getOutputProperty(), odMatrixIterator, mode, timePeriod, outputTimeUnit.getMultiplier()))
								.map(outValue -> OutputUtils.formatObject(outValue)).collect(Collectors.toList());
						csvPrinter.printRecord(rowValues);
					}
				}
			}
		} catch (Exception e) {
			return new PlanItException(e);
		}
		return null;
	}
	
	/**
	 * Write output values to the Path CSV file for the current iteration
	 * 
	 * @param outputTypeConfiguration output type configuration for the current output type
	 * @param outputAdapter output adapter for the current output type
	 * @param modes         Set of modes for the current assignment
	 * @param timePeriod    the current time period
	 * @param csvPrinter    CSVPrinter object to record results for this iteration
	 * @return PlanItException thrown if the CSV file cannot be created or written
	 *         to
	 */
	protected PlanItException writePathResultsForCurrentTimePeriodToCsvPrinter(OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, CSVPrinter csvPrinter) {
		try {
			ODPathOutputTypeAdapter odPathOutputTypeAdapter = (ODPathOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputTypeConfiguration.getOutputType());
			SortedSet<BaseOutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();
			for (Mode mode : modes) {
				ODPath odPath = odPathOutputTypeAdapter.getODPath(mode);
				for (ODPathIterator odPathIterator = odPath.iterator(); odPathIterator.hasNext(); ) {
					odPathIterator.next();
					List<Object> rowValues = outputProperties.stream()
							.map(outputProperty -> odPathOutputTypeAdapter.getODPathOutputPropertyValue(outputProperty.getOutputProperty(), odPathIterator, mode, timePeriod))
							.map(outValue -> OutputUtils.formatObject(outValue)).collect(Collectors.toList());
					csvPrinter.printRecord(rowValues);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new PlanItException(e);
		}
		return null;
	}

	/**
	 * Write output values to the Link CSV file for the current iteration
	 * 
	 * @param outputTypeConfiguration the current output type configuration
	 * @param outputAdapter output adapter for the current output type
	 * @param modes         Set of modes for the current assignment
	 * @param timePeriod    the current time period
	 * @param csvPrinter    CSVPrinter object to record results for this iteration
	 * @return PlanItException thrown if the CSV file cannot be created or written to
	 */
	protected PlanItException writeLinkResultsForCurrentTimePeriodToCsvPrinter(OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod, CSVPrinter csvPrinter) {
		try {
			LinkOutputTypeAdapter linkOutputTypeAdapter = (LinkOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputTypeConfiguration.getOutputType());
			SortedSet<BaseOutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();
			for (Mode mode : modes) {
				for (LinkSegment linkSegment : linkOutputTypeAdapter.getLinkSegments()) {
					if (outputTypeConfiguration.isRecordLinksWithZeroFlow() || linkOutputTypeAdapter.isFlowPositive(linkSegment, mode)) {
						List<Object> rowValues = outputProperties.stream()
								.map(outputProperty -> linkOutputTypeAdapter.getLinkOutputPropertyValue(outputProperty.getOutputProperty(), linkSegment, mode, timePeriod, outputTimeUnit.getMultiplier()))
								.map(outValue -> OutputUtils.formatObject(outValue)).collect(Collectors.toList());
						csvPrinter.printRecord(rowValues);
					}
				}
			}
		} catch (Exception e) {
			return new PlanItException(e);
		}
		return null;
	}

	/**
	 * Open the CSV output file and write the headers to it
	 * 
	 * @param outputTypeConfiguration the current output type configuration
	 * @param csvFileName      the name of the CSV output file
	 * @return the CSVPrinter object (output values will be written to this in subsequent rows)
	 * @throws Exception thrown if there is an error opening the file
	 */
	protected CSVPrinter openCsvFileAndWriteHeaders(OutputTypeConfiguration outputTypeConfiguration, String csvFileName) throws Exception {
		CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(csvFileName), CSVFormat.DEFAULT.withEscape(Character.MIN_VALUE).withQuoteMode(QuoteMode.NONE));
		List<String> headerValues = outputTypeConfiguration.getOutputProperties().stream().map(BaseOutputProperty::getName).collect(Collectors.toList());
		csvPrinter.printRecord(headerValues);
		return csvPrinter;
	}

	/**
	 * Add a new name of the CSV output file for a specified output type
	 * 
	 * @param outputType     the specified output type
	 * @param outputFileName the name of the output file to be added
	 */
	public void addCsvFileNamePerOutputType(OutputType outputType, String csvFileName) {
		if (!csvFileNameMap.containsKey(outputType)) {
			csvFileNameMap.put(outputType, new ArrayList<String>());
		}
		csvFileNameMap.get(outputType).add(csvFileName);
	}

	/**
	 * Constructor
	 */
	public CsvFileOutputFormatter() {
		csvFileNameMap = new HashMap<OutputType, List<String>>();
	}
}