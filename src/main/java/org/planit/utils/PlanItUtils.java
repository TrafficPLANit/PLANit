package org.planit.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.planit.dto.BprResultDto;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

public class PlanItUtils {
	
	public static final double DEFAULT_EPSILON = 0.000001;

	public static void saveResultsToCsvFile(SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap, String resultsFileLocation) throws IOException {

		File existingFile = new File(resultsFileLocation);
		if (existingFile.exists()) {
			existingFile.delete();
		}
		Writer writer = new FileWriter(resultsFileLocation);
		CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL);
		printer.printRecord("Run Id", "Time Period Id", "Mode Id", "Start Node Id", "End Node Id", "Link Flow", "Capacity", "Length", "Speed", "Link Cost",  "Cost to End Node", "alpha", "beta");
		for (Long runId : resultsMap.keySet()) {
			for (TimePeriod timePeriod : resultsMap.get(runId).keySet()) {
				for (Mode mode : resultsMap.get(runId).get(timePeriod).keySet()) {
					for (BprResultDto resultDto : resultsMap.get(runId).get(timePeriod).get(mode)) {
						printer.printRecord(runId, 
								                        timePeriod.getId(), 
								                        mode.getId(), 
								                        resultDto.getStartNodeId(),
								                        resultDto.getEndNodeId(), 
								                        resultDto.getLinkFlow(), 
								                        resultDto.getCapacity(),
								                        resultDto.getLength(),
								                        resultDto.getSpeed(),
								                        resultDto.getLinkCost(), 
								                        resultDto.getTotalCostToEndNode(),
								                        resultDto.getAlpha(),
								                        resultDto.getBeta());
					}
				}
			}	
		} 
		printer.close();
		writer.close();
	}
	
				
	public static SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> createResultsMapFromCsvFile(String resultsFileLocation) throws IOException {
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>>();
		Reader in = new FileReader(resultsFileLocation);
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);
		for (CSVRecord record : records) {
			long runId = Long.parseLong(record.get("Run Id"));
			if (!resultsMap.containsKey(runId)) {
				resultsMap.put(runId, new TreeMap<TimePeriod,SortedMap<Mode, SortedSet<BprResultDto>>>());
			}
			long timePeriodId = Long.parseLong(record.get("Time Period Id"));
			TimePeriod timePeriod = TimePeriod.getById(timePeriodId);
			if (!resultsMap.get(runId).containsKey(timePeriod)) {
				resultsMap.get(runId).put(timePeriod, new TreeMap<Mode, SortedSet<BprResultDto>>());
			}			
			long modeId = Long.parseLong(record.get("Mode Id"));
			Mode mode = Mode.getById(modeId);
			if (!resultsMap.get(runId).get(timePeriod).containsKey(mode)) {
				resultsMap.get(runId).get(timePeriod).put(mode, new TreeSet<BprResultDto>());
			}
			long startNodeId = Long.parseLong(record.get("Start Node Id"));
			long endNodeId = Long.parseLong(record.get("End Node Id"));
			double linkFlow = Double.parseDouble(record.get("Link Flow"));
			double linkCost = Double.parseDouble(record.get("Link Cost"));
			double totalCostToEndNode = Double.parseDouble(record.get("Cost to End Node"));
			double capacity = Double.parseDouble(record.get("Capacity"));
			double length = Double.parseDouble(record.get("Length"));
			double speed = Double.parseDouble(record.get("Speed"));
			double alpha = Double.parseDouble(record.get("alpha"));
			double beta = Double.parseDouble(record.get("beta"));
			BprResultDto resultDto = new BprResultDto(startNodeId, endNodeId, linkFlow, linkCost, totalCostToEndNode, capacity, length, speed, alpha, beta);
			resultsMap.get(runId).get(timePeriod).get(mode).add(resultDto);
		}
		in.close();
		return resultsMap;
	}
	
}
