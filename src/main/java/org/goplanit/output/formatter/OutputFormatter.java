package org.goplanit.output.formatter;

import java.util.Set;

import org.goplanit.output.adapter.OutputAdapter;
import org.goplanit.output.adapter.OutputTypeAdapter;
import org.goplanit.output.configuration.OutputConfiguration;
import org.goplanit.output.configuration.OutputTypeConfiguration;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;

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
  public static final String NOT_AVAILABLE = OutputTypeAdapter.PROPERTY_NOT_AVAILABLE;

  /**
   * Default output formatter supported by PLANit from PLANitIO repository
   */
  public static final String PLANIT_OUTPUT_FORMATTER = "org.goplanit.io.output.formatter.PlanItOutputFormatter";

  /**
   * Memory output formatter which stores all results in memory rather than persist them to disk
   */
  public static final String MEMORY_OUTPUT_FORMATTER = MemoryOutputFormatter.class.getCanonicalName();

  /**
   * Collect the id of the formatter
   * 
   * @return id
   */
  public long getId();

  /**
   * Persist the output data based on the passed in configuration and adapter (contained in the configuration)
   * 
   * @param timePeriod              TimePeriod for the assignment to be saved
   * @param modes                   Set of modes for the assignment to be saved
   * @param outputConfiguration     output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to be saved
   * @param outputAdapter           OutputAdapter for the assignment to be saved
   */
  public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter);

  /**
   * Open resources to store results
   * 
   * @param outputConfiguration OutputTypeConfiguration for the assignment to be saved
   * @param runId               the id number of the run
   * @throws PlanItException thrown if there is an error
   */
  public void initialiseBeforeSimulation(OutputConfiguration outputConfiguration, long runId) throws PlanItException;

  /**
   * Close resources to store results
   * 
   * @param outputConfiguration OutputTypeConfiguration for the assignment to be saved
   * @param outputAdapter       the outputAdapter
   * @param timePeriod the last time period used before simulation ended
   * @param iterationIndex the last iteration index of the last time period used before the simulation eneded
   */
  public void finaliseAfterSimulation(
          OutputConfiguration outputConfiguration, OutputAdapter outputAdapter, TimePeriod timePeriod, int iterationIndex);

  /**
   * Flag to indicate whether an implementation can handle multiple iterations
   * 
   * If this returns false, acts as though OutputConfiguration.setPersistOnlyFinalIteration() is set to true
   * 
   * @return flag to indicate whether the OutputFormatter can handle multiple iterations
   */
  public boolean canHandleMultipleIterations();

}