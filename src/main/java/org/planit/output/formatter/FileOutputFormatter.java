package org.planit.output.formatter;

import java.io.File;
import java.nio.file.Files;

import org.planit.exceptions.PlanItException;
import org.planit.output.OutputType;
import org.planit.time.TimePeriod;

public abstract class FileOutputFormatter extends BaseOutputFormatter {

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
	
}
