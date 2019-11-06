package org.planit.output.formatter;

import java.util.Set;

import org.planit.exceptions.PlanItException;
import org.planit.output.adapter.OutputAdapter;
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
	 * Constant to report that an output value has not been set
	 */
	public static final String NOT_SPECIFIED = "Not Specified";
	
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
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to be saved
	 * @param outputAdapter OutputAdapter for the assignment to be saved
	 * @throws PlanItException thrown if there is an error
	 */
	public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter) throws PlanItException;

	/**
	 * Open resources to store results
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to  be saved
	 * @param runId the id number of the run
	 * @throws PlanItException thrown if there is an error
	 */
	public void open(OutputTypeConfiguration outputTypeConfiguration, long runId) throws PlanItException;

	/**
	 * Close resources to store results
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to  be saved
	 * @throws PlanItException thrown if there is an error
	 */
	public void close(OutputTypeConfiguration outputTypeConfiguration) throws PlanItException;
	
	/**
	 * Flag to indicate whether an implementation can handle multiple iterations
	 * 
	 * If this returns false, acts as though OutputConfiguration.setPersistOnlyFinalIteration() is set to true
	 * 
	 * @return flag to indicate whether the OutputFormatter can handle multiple iterations
	 */
	public boolean canHandleMultipleIterations();
	
}