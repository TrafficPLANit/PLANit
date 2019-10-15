package org.planit.output.formatter;

import java.util.List;

import org.planit.output.OutputType;

/**
 * This interfaces defines behaviours which output formatters which generate a CSV output file must have.
 * 
 * @author gman6028
 *
 */
public interface CsvTextFileOutputFormatter {
	
	/**
	 * Returns a list of  names of CSV output file for a specified output type
	 * 
	 * @param outputType the specified output type
	 * @return the name of the output file
	 */
	public List<String> getCsvFileName(OutputType outputType);
	
	/**
	 * Add a new name of a CSV output file for a specified output type
	 * 
	 * @param outputType the specified output type
	 * @param csvFileName the name of the output file to be added for the specified output type
	 */
	public void addCsvFileNamePerOutputType(OutputType outputType, String csvFileName);
	
	/**
	 * Set the root name of the CSV output file for all output types
	 * 
	 * @param csvNameRoot the root name of the output file
	 */
	public void setCsvNameRoot(String csvNameRoot);
	
	/**
	 * Sets the extension of the CSV output file name for all output types
	 * 
	 * @param csvNameExtension the extension of the output file names
	 */
	public void setCsvNameExtension(String csvNameExtension);
	
	/**
	 * Sets the directory of the CSV output files for all output types
	 * 
	 * @param csvOutputDirectory the directory of the output files
	 */
	public void setCsvDirectory(String csvOutputDirectory);

}
