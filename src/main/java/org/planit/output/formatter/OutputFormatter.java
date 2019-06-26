package org.planit.output.formatter;

import java.util.Set;

import org.planit.data.SimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

/**
 * Interface for persisting output data in a particular format
 * 
 * @author markr
 *
 */
public interface OutputFormatter {

	/**
	 * Collect the id of the formatter
	 * 
	 * @return id
	 */
	public long getId();

	/**
	 * Persist the output data based on the passed in configuration and adapter
	 * (contained in the configuration)
	 * 
	 * @param timePeriod              TimePeriod for the assignment to be saved
	 * @param modes                   Set of modes for the assignment to be saved
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to
	 *                                be saved
	 * @param simulationData          simulation data for the current iteration
	 * @throws PlanItException thrown if there is an error
	 */
	public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration,
			SimulationData simulationData) throws PlanItException;

	/**
	 * Open resources to store results
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to
	 *                                be saved
	 * @throws PlanItException thrown if there is an error opening the resources
	 */
	public void open(OutputTypeConfiguration outputTypeConfiguration) throws PlanItException;

	/**
	 * Close resources to store results
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to
	 *                                be saved
	 * @throws PlanItException thrown if there is an error closing the resources
	 */
	public void close(OutputTypeConfiguration outputTypeConfiguration) throws PlanItException;

}
