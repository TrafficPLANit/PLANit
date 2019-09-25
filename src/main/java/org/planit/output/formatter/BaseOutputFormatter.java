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

	private static final OutputTimeUnit DEFAULT_TIME_UNIT = OutputTimeUnit.HOURS;

	/**
	 * Unique internal id of the output writer
	 */
	protected long id;

	/**
	 * Time unit to be used in outputs
	 */
	protected OutputTimeUnit outputTimeUnit;

	/**
	 * Constructor,
	 */
	public BaseOutputFormatter() {
		this.id = IdGenerator.generateId(OutputManager.class);
		outputTimeUnit = DEFAULT_TIME_UNIT;
	}

	// getters - setters

	public long getId() {
		return id;
	}

	public OutputTimeUnit getOutputTimeUnit() {
		return outputTimeUnit;
	}

	public void setOutputTimeUnit(OutputTimeUnit outputTimeUnit) {
		this.outputTimeUnit = outputTimeUnit;
	}
	
	public String getOutputTimeUnitString() {
		return outputTimeUnit.value();
	}

	public double getTimeUnitMultiplier() {
		switch (outputTimeUnit) {
		case HOURS:
			return 1.0;
		case MINUTES:
			return 60.0;
		case SECONDS:
			return 3600.0;
		}
		return -1.0;
	}

}
