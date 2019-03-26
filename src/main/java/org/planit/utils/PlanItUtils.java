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
import org.planit.dto.ResultDto;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

public class PlanItUtils {
	
	public static final double DEFAULT_EPSILON = 0.000001;

	public static void saveResultsToCsvFile(SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<ResultDto>>>> resultsMap, String resultsFileLocation) throws IOException {

		File existingFile = new File(resultsFileLocation);
		if (existingFile.exists()) {
			existingFile.delete();
		}
		Writer writer = new FileWriter(resultsFileLocation);
		CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL);
		printer.printRecord("Run Id", "Time Period Id", "Mode Id", "Start Node Id", "End Node Id", "Link Flow", "Link Cost",  "Cost to End Node");
		for (Long runId : resultsMap.keySet()) {
			for (TimePeriod timePeriod : resultsMap.get(runId).keySet()) {
				for (Mode mode : resultsMap.get(runId).get(timePeriod).keySet()) {
					for (ResultDto resultDto : resultsMap.get(runId).get(timePeriod).get(mode)) {
						printer.printRecord(runId, timePeriod.getId(), mode.getId(), resultDto.getStartNodeId(),resultDto.getEndNodeId(), resultDto.getLinkFlow(), resultDto.getLinkCost(), resultDto.getTotalCostToEndNode());
					}
				}
			}	
		} 
		printer.close();
		writer.close();
	}
	
				
	public static SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<ResultDto>>>> createResultsMapFromCsvFile(String resultsFileLocation) throws IOException {
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<ResultDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<ResultDto>>>>();
		Reader in = new FileReader(resultsFileLocation);
		Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);
		for (CSVRecord record : records) {
			long runId = Long.parseLong(record.get("Run Id"));
			if (!resultsMap.containsKey(runId)) {
				resultsMap.put(runId, new TreeMap<TimePeriod,SortedMap<Mode, SortedSet<ResultDto>>>());
			}
			long timePeriodId = Long.parseLong(record.get("Time Period Id"));
			TimePeriod timePeriod = TimePeriod.getById(timePeriodId);
			if (!resultsMap.get(runId).containsKey(timePeriod)) {
				resultsMap.get(runId).put(timePeriod, new TreeMap<Mode, SortedSet<ResultDto>>());
			}			
			long modeId = Long.parseLong(record.get("Mode Id"));
			Mode mode = Mode.getById(modeId);
			if (!resultsMap.get(runId).get(timePeriod).containsKey(mode)) {
				resultsMap.get(runId).get(timePeriod).put(mode, new TreeSet<ResultDto>());
			}
			long startNodeId = Long.parseLong(record.get("Start Node Id"));
			long endNodeId = Long.parseLong(record.get("End Node Id"));
			double linkFlow = Double.parseDouble(record.get("Link Flow"));
			double linkCost = Double.parseDouble(record.get("Link Cost"));
			double totalCostToEndNode = Double.parseDouble(record.get("Cost to End Node"));
			ResultDto resultDto = new ResultDto(startNodeId, endNodeId, linkFlow, linkCost, totalCostToEndNode);
			resultsMap.get(runId).get(timePeriod).get(mode).add(resultDto);
		}
		in.close();
		return resultsMap;
	}

}
