package org.planit.output.formatter;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.network.transport.TransportNetwork;
import org.planit.odmatrix.ODMatrixIterator;
import org.planit.odmatrix.skim.ODSkimMatrix;
import org.planit.output.OutputType;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentODOutputAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.property.BaseOutputProperty;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.utils.OutputUtils;

/**
 * Common methods used by output formatters which write data to physical files
 * 
 * @author gman6028
 *
 */
public abstract class FileOutputFormatter extends BaseOutputFormatter {

	/**
	 * Map of list of  CSV output file names for each OutputType
	 */
	protected Map<OutputType, List<String>> csvFileNameMap;

	/**
	 * Generates the name of an output file.
	 * 
	 * @param outputDirectory location output files are to be written
	 * @param nameRoot        root name of the output files
	 * @param nameExtension   extension of the output files
	 * @param outputType the OutputType of the output
	 * @param runId the id of the traffic assignment run
	 * @param iteration       current iteration
	 * @return the name of the output file
	 * @throws PlanItException thrown if the output directory cannot be opened
	 */
	protected String generateOutputFileName(String outputDirectory, String nameRoot, String nameExtension, TimePeriod timePeriod, OutputType outputType, long runId, int iteration) throws PlanItException {
		try {
			File directory = new File(outputDirectory);
			if (!directory.isDirectory()) {
				Files.createDirectories(directory.toPath());
			}
			String newFileName = null;
			if (timePeriod == null) {
				if (iteration == -1) {
					newFileName = outputDirectory + "\\" + outputType.value() + "_RunId " + runId + "_" + nameRoot + nameExtension; 
				} else {
					newFileName = outputDirectory + "\\" + outputType.value() + "_RunId " + runId +  "_" + nameRoot + "_" + iteration + nameExtension; 
				}
			} else {
				if (iteration == -1) {
					newFileName = outputDirectory + "\\" + outputType.value() + "_RunId " + runId + "_" + nameRoot + "_" + timePeriod.getDescription() + nameExtension;
				} else {
					newFileName = outputDirectory + "\\" + outputType.value() + "_RunId " + runId + "_" + nameRoot + "_" + timePeriod.getDescription() + "_" + iteration	+ nameExtension;
				}
			} 
			return newFileName;
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}
	
	/**
	 * Generates the name of an output file.
	 * 
	 * @param outputDirectory location output files are to be written
	 * @param nameRoot        root name of the output files
	 * @param nameExtension   extension of the output files
	 * @param outputType the OutputType of the output
	 * @param runId the id of the traffic assignment run
	 * @return the name of the output file
	 * @throws PlanItException thrown if the output directory cannot be opened
	 */
	protected String generateOutputFileName(String outputDirectory, String nameRoot, String nameExtension, TimePeriod timePeriod, OutputType outputType, long runId) throws PlanItException {
		return generateOutputFileName(outputDirectory, nameRoot, nameExtension, timePeriod, outputType, runId, -1);
	}
	
	/**
	 * Write output values to the OD CSV file for the current iteration
	 * 
	 * @param outputAdapter outputAdapter storing network
	 * @param modes         Set of modes for the current assignment
	 * @param timePeriod    the current time period
	 * @param csvPrinter    CSVPrinter object to record results for this iteration
	 * @return PlanItException thrown if the CSV file cannot be created or written to
	 */
	protected PlanItException writeOdResultsForCurrentModeAndTimePeriodToCsvPrinter(OutputTypeConfiguration outputTypeConfiguration, Set<Mode> modes, TimePeriod timePeriod, CSVPrinter csvPrinter) {
		try {
			TraditionalStaticAssignmentODOutputAdapter traditionalStaticAssignmentODOutputAdapter = (TraditionalStaticAssignmentODOutputAdapter) outputTypeConfiguration.getOutputAdapter();
			TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = (TraditionalStaticAssignmentSimulationData) traditionalStaticAssignmentODOutputAdapter
					.getSimulationData();
			SortedSet<BaseOutputProperty> outputProperties = traditionalStaticAssignmentODOutputAdapter.getOutputProperties();
			for (Mode mode : modes) {
				ODSkimMatrix odSkimMatrix = traditionalStaticAssignmentSimulationData.getODSkimMatrix(mode);
				ODMatrixIterator odMatrixIterator = odSkimMatrix.iterator();
				while (odMatrixIterator.hasNext()) {
					odMatrixIterator.next();
					List<Object> rowValues = outputProperties.stream()
							.map(outputProperty -> traditionalStaticAssignmentODOutputAdapter.getOdPropertyValue(outputProperty, odMatrixIterator, mode, timePeriod))
							.map(outValue -> OutputUtils.formatObject(outValue))
							.collect(Collectors.toList());
					csvPrinter.printRecord(rowValues);
				}
			}
		} catch (Exception e) {
			return new PlanItException(e);
		}
		return null;
	}

	/**
	 * Write output values to the Link CSV file for the current iteration
	 * 
	 * @param outputAdapter outputAdapter storing network
	 * @param modes         Set of modes for the current assignment
	 * @param timePeriod    the current time period
	 * @param csvPrinter    CSVPrinter object to record results for this iteration
	 * @return PlanItException thrown if the CSV file cannot be created or written to
	 */
	protected PlanItException writeLinkResultsForCurrentModeAndTimePeriodToCsvPrinter(OutputTypeConfiguration outputTypeConfiguration, Set<Mode> modes, TimePeriod timePeriod, CSVPrinter csvPrinter) {
		try {
			TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter = (TraditionalStaticAssignmentLinkOutputAdapter) outputTypeConfiguration.getOutputAdapter();
			TransportNetwork transportNetwork = traditionalStaticAssignmentLinkOutputAdapter.getTransportNetwork();
			SortedSet<BaseOutputProperty> outputProperties = traditionalStaticAssignmentLinkOutputAdapter.getOutputProperties();
			for (Mode mode : modes) {
				Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();
				while (linkSegmentIter.hasNext()) {
					MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) linkSegmentIter.next();
					if (traditionalStaticAssignmentLinkOutputAdapter.isFlowPositive(linkSegment, mode)) {
						List<Object> rowValues = outputProperties.stream()
								.map(outputProperty -> traditionalStaticAssignmentLinkOutputAdapter.getLinkPropertyValue(outputProperty.getOutputProperty(), linkSegment, mode, timePeriod))
								.map(outValue -> OutputUtils.formatObject(outValue))
								.collect(Collectors.toList());
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
	 * @param outputProperties the output properties to be included as columns in
	 *                         the CSV file
	 * @param csvFileName      the name of the CSV output file
	 * @return the CSVPrinter object (output values will be written to this in
	 *         subsequent rows)
	 * @throws Exception thrown if there is an error opening the file
	 */
	protected CSVPrinter openCsvFileAndWriterHeaders(OutputAdapter outputAdapter, String csvFileName) throws Exception {
		CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(csvFileName), CSVFormat.EXCEL);
		List<String> headerValues = outputAdapter.getOutputProperties().stream().map(BaseOutputProperty::getName).collect(Collectors.toList());
		csvPrinter.printRecord(headerValues);
		return csvPrinter;
	}

	/**
	 * Add a new  name of the CSV output file for a specified output type
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
	public FileOutputFormatter() {
		csvFileNameMap = new HashMap<OutputType, List<String>>();
	}

}
