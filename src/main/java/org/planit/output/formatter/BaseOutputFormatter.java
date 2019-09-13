package org.planit.output.formatter;

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
