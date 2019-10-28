package org.planit.output.formatter;

import java.io.FileWriter;
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
import org.planit.od.odmatrix.ODMatrixIterator;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentODOutputAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.ODSkimOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.property.BaseOutputProperty;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.utils.OutputUtils;

public abstract class CsvFileOutputFormatter extends FileOutputFormatter {

	/**
	 * Map of list of CSV output file names for each OutputType
	 */
	protected Map<OutputType, List<String>> csvFileNameMap;

	/**
	 * Write output values to the OD CSV file for the current iteration
	 * 
	 * @param outputAdapter outputAdapter storing network
	 * @param modes         Set of modes for the current assignment
	 * @param timePeriod    the current time period
	 * @param csvPrinter    CSVPrinter object to record results for this iteration
	 * @return PlanItException thrown if the CSV file cannot be created or written
	 *         to
	 */
	protected PlanItException writeOdResultsForCurrentTimePeriodToCsvPrinter(
			OutputTypeConfiguration outputTypeConfiguration, Set<Mode> modes, TimePeriod timePeriod,
			CSVPrinter csvPrinter) {
		try {
			TraditionalStaticAssignmentODOutputAdapter traditionalStaticAssignmentODOutputAdapter = (TraditionalStaticAssignmentODOutputAdapter) outputTypeConfiguration
					.getOutputAdapter();
			TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = (TraditionalStaticAssignmentSimulationData) traditionalStaticAssignmentODOutputAdapter
					.getSimulationData();
			SortedSet<BaseOutputProperty> outputProperties = traditionalStaticAssignmentODOutputAdapter
					.getOutputProperties();
			for (ODSkimOutputType odSkimOutputType : traditionalStaticAssignmentSimulationData
					.getActiveSkimOutputTypes()) {
				for (Mode mode : modes) {
					ODSkimMatrix odSkimMatrix = traditionalStaticAssignmentSimulationData
							.getODSkimMatrix(odSkimOutputType, mode);
					ODMatrixIterator odMatrixIterator = odSkimMatrix.iterator();
					while (odMatrixIterator.hasNext()) {
						odMatrixIterator.next();
						List<Object> rowValues = outputProperties.stream()
								.map(outputProperty -> traditionalStaticAssignmentODOutputAdapter.getOdPropertyValue(
										outputProperty.getOutputProperty(), odMatrixIterator, mode, timePeriod,
										outputTimeUnit.getMultiplier()))
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
	 * Write output values to the Link CSV file for the current iteration
	 * 
	 * @param outputAdapter outputAdapter storing network
	 * @param modes         Set of modes for the current assignment
	 * @param timePeriod    the current time period
	 * @param csvPrinter    CSVPrinter object to record results for this iteration
	 * @return PlanItException thrown if the CSV file cannot be created or written
	 *         to
	 */
	protected PlanItException writeLinkResultsForCurrentTimePeriodToCsvPrinter(
			OutputTypeConfiguration outputTypeConfiguration, Set<Mode> modes, TimePeriod timePeriod,
			CSVPrinter csvPrinter) {
		TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter = (TraditionalStaticAssignmentLinkOutputAdapter) outputTypeConfiguration
				.getOutputAdapter();
		TransportNetwork transportNetwork = traditionalStaticAssignmentLinkOutputAdapter.getTransportNetwork();
		try {
			SortedSet<BaseOutputProperty> outputProperties = traditionalStaticAssignmentLinkOutputAdapter
					.getOutputProperties();
			for (Mode mode : modes) {
				Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();
				while (linkSegmentIter.hasNext()) {
					MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) linkSegmentIter.next();
					if (traditionalStaticAssignmentLinkOutputAdapter.isFlowPositive(linkSegment, mode)) {
						List<Object> rowValues = outputProperties.stream()
								.map(outputProperty -> traditionalStaticAssignmentLinkOutputAdapter
										.getLinkPropertyValue(outputProperty.getOutputProperty(), linkSegment, mode,
												timePeriod, outputTimeUnit.getMultiplier()))
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
	 * @param outputProperties the output properties to be included as columns in
	 *                         the CSV file
	 * @param csvFileName      the name of the CSV output file
	 * @return the CSVPrinter object (output values will be written to this in
	 *         subsequent rows)
	 * @throws Exception thrown if there is an error opening the file
	 */
	protected CSVPrinter openCsvFileAndWriteHeaders(OutputAdapter outputAdapter, String csvFileName) throws Exception {
		CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(csvFileName), CSVFormat.EXCEL);
		List<String> headerValues = outputAdapter.getOutputProperties().stream().map(BaseOutputProperty::getName)
				.collect(Collectors.toList());
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
