package org.planit.output.formatter;

import java.util.logging.Logger;

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

	private static final Logger LOGGER = Logger.getLogger(BaseOutputFormatter.class.getName());
	/**
	 * Unique internal id of the output writer
	 */
	protected long id;

	/**
	 * Constructor,
	 */
	public BaseOutputFormatter() {
		this.id = IdGenerator.generateId(OutputManager.class);
	}

	// getters - setters

	public long getId() {
		return id;
	}

}
