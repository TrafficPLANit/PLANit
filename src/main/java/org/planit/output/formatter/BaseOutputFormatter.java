package org.planit.output.formatter;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.planit.exceptions.PlanItException;
import org.planit.output.OutputManager;
import org.planit.utils.IdGenerator;

/**
 * Base class for all formatters of output data, i.e. persistence of certain
 * types of data into a particular format
 * 
 * @author markr
 *
 */
public abstract class BaseOutputFormatter implements OutputFormatter {

	/**
	 * Unique internal id of the output writer
	 */
	protected long id;

	/**
	 * Description to be included in the XML file in the <description> element
	 */
	protected String description;

	/**
	 * Version to be included in the XML file in the <version> element
	 */
	protected String version;

	/**
	 * Base constructor
	 * 
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public BaseOutputFormatter() throws PlanItException {
		//try {
			this.id = IdGenerator.generateId(OutputManager.class);
			try (InputStream input = BaseOutputFormatter.class.getClassLoader().getResourceAsStream("application.properties")) {

	            Properties prop = new Properties();

	            if (input == null) {
	                System.out.println("Sorry, unable to find application.properties");
	                return;
	            }

	            //load a properties file from class path, inside static method
	            prop.load(input);

	            description = prop.getProperty("planit.description");
	            version = prop.getProperty("planit.version");

		} catch (Exception e) {
			throw new PlanItException(e);
		}

	}

	// getters - setters

	public long getId() {
		return id;
	}

	/**
	 * Allows the developer to set the String to be used in the <description> element in the output XML file
	 * 
	 * If this method is not called, the contents of the <description> element in the PlanIt POM file is used.
	 * 
	 * @param description description to be included
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Allows the developer to set the String to be used in the <version> element in the output XML file
	 * 
	 * If this method is not called, the contents of the <version> element in the PlanIt POM file is used.
	 * 
	 * @param version version to be included
	 */
	public void setVersion(String version) {
		this.version = version;
	}

}
