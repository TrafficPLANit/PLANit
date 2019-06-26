package org.planit.output.formatter;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

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

	private static final String DEFAULT_PROPERTIES_FILE_NAME = "application.properties";
	private static final String DEFAULT_DESCRIPTION_PROPERTY_NAME = "planit.description";
	private static final String DEFAULT_VERSION_PROPERTY_NAME = "planit.version";

	private static final Logger LOGGER = Logger.getLogger(BaseOutputFormatter.class.getName());
	/**
	 * Unique internal id of the output writer
	 */
	protected long id;

	/**
	 * Description property to be included in the output files
	 */
	protected String description;

	/**
	 * Version property to be included in the output files
	 */
	protected String version;

	/**
	 * Constructor, uses default values for properties file name, description
	 * property and version property
	 * 
	 * @throws PlanItException thrown if the application properties file exists but
	 *                         cannot be opened
	 */
	public BaseOutputFormatter() throws PlanItException {
		this(DEFAULT_PROPERTIES_FILE_NAME, DEFAULT_DESCRIPTION_PROPERTY_NAME, DEFAULT_VERSION_PROPERTY_NAME);
	}

	/**
	 * Constructor, uses default values description property and version property
	 * 
	 * @param propertiesFileName the name of the application properties file
	 * @throws PlanItException thrown if the application properties file exists but
	 *                         cannot be opened
	 */
	public BaseOutputFormatter(String propertiesFileName) throws PlanItException {
		this(propertiesFileName, DEFAULT_DESCRIPTION_PROPERTY_NAME, DEFAULT_VERSION_PROPERTY_NAME);
	}

	/**
	 * Constructor
	 * 
	 * @param propertiesFileName  the name of the application properties file
	 * @param descriptionProperty the name of the description property
	 * @param versionProperty     the name of the version property
	 * @throws PlanItException thrown if the application properties file exists but
	 *                         cannot be opened
	 */
	public BaseOutputFormatter(String propertiesFileName, String descriptionProperty, String versionProperty)
			throws PlanItException {
		this.id = IdGenerator.generateId(OutputManager.class);
		if (propertiesFileName == null) {
			LOGGER.info(
					"No application properties file specified, version and description properties must be set from the code or will not be recorded.");
			return;
		}
		try (InputStream input = BaseOutputFormatter.class.getClassLoader().getResourceAsStream(propertiesFileName)) {

			if (input == null) {
				LOGGER.info("Application properties " + propertiesFileName
						+ " could not be found, version and description properties must be set from the code or will not be recorded.");
				return;
			}

			// load a properties file from class path, inside static method
			Properties prop = new Properties();
			prop.load(input);

			description = prop.getProperty(descriptionProperty);
			if (description == null) {
				LOGGER.info("Description property could not be set from properties file " + propertiesFileName
						+ ", this must be set from the code or will not be recorded.");
			}
			version = prop.getProperty(versionProperty);
			if (version == null) {
				LOGGER.info("Version property could not be set from properties file " + propertiesFileName
						+ ", this must be set from the code or will not be recorded.");
			}

		} catch (Exception e) {
			throw new PlanItException(e);
		}

	}

	// getters - setters

	public long getId() {
		return id;
	}

	/**
	 * Allows the developer to set the output description property
	 * 
	 * @param description description to be included
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Allows the developer to set version output property
	 * 
	 * @param version version to be included
	 */
	public void setVersion(String version) {
		this.version = version;
	}

}
