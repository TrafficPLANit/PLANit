package org.planit.output.formatter;

import org.planit.output.enums.OutputType;

/**
 * This interfaces defines behaviours which output formatters which generate an XML output file must have.
 * 
 * @author gman6028
 *
 */
public interface XmlTextFileOutputFormatter {
	
	/**
	 * Returns the XML output file name for a specified output type
	 * 
	 * @param outputType the specified output type
	 * @return the name of the output file
	 */
	public String getXmlFileName(OutputType outputType);
	
	/**
	 * Set the name of an XML  output file for a specified output type
	 * 
	 * @param outputType the specified output type
	 * @param xmlFileName the name of the output file to be added for the specified output type
	 */
	public void setXmlFileNamePerOutputType(OutputType outputType, String xmlFileName);
	
	/**
	 * Set the root name of the XML output file for all output types
	 * 
	 * @param xmlNameRoot the root name of the output file
	 */
	public void setXmlNameRoot(String xmlNameRoot);
	
	/**
	 * Sets the extension of the XML output file name for all output types
	 * 
	 * @param xmlNameExtension the extension of the output file names
	 */
	public void setXmlNameExtension(String xmlNameExtension);
	
	/**
	 * Sets the directory of the XML output files for all output types
	 * 
	 * @param xmlOutputDirectory the directory of the output files
	 */
	public void setXmlDirectory(String xmlOutputDirectory);

}
