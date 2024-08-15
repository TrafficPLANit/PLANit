package org.goplanit.output.adapter;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;

import java.util.List;
import java.util.Optional;

/**
 * Top-level abstract class which defines the common methods required by simulation output type adapters
 * 
 * @author markr
 *
 */
public abstract class SimulationOutputTypeAdapterImpl extends OutputTypeAdapterImpl implements SimulationOutputTypeAdapter {

  protected Optional<?> getUnavailableOutputType() {
    // stub implementation so unsupporting deriving assignments won't crash when activated
    return Optional.of("N/A");
  }

  /**
   *  Access to rotue choice iteration run time
   *
   * @return run time for latest route choice iteration
   */
  protected Optional<?> getRouteChoiceIterationRunTime() {
    return getUnavailableOutputType();
  }

  /**
   *  Access to current total path count (if relevant)
   *
   * @return total path count
   */
  protected Optional<?> getTotalPathCount(Mode mode) {
    return getUnavailableOutputType();
  }

  /**
   *  Access to current added path count (if relevant)
   *
   * @return added path count
   */
  protected Optional<?> getAddedPathCount(Mode mode) {
    return getUnavailableOutputType();
  }

  /**
   *  Access to current removed path count (if relevant)
   *
   * @return removed path count
   */
  protected Optional<?> getRemovedPathCount(Mode mode) {
    return getUnavailableOutputType();
  }


  /**
   * Collect output property values that do not depend on simulation information directly.
   * If no match is found  an empty option is returned (currently always)
   *
   * @param outputProperty to collect
   * @return result (if any match)
   */
  protected Optional<?> getSimulationIndependentPropertyValue(
          OutputProperty outputProperty) {

    Optional<?> value = null;
    switch (outputProperty.getOutputPropertyType()) {
      default:
        value = Optional.empty();
    }
    return value;
  }

  /**
   * Collect output property values that do depend on simulation information directly.
   * If no match is found  an empty option is returned
   *
   * @param outputProperty to collect
   * @param mode to use
   * @return result (if any match)
   */
  protected Optional<?> getSimulationDependentPropertyValue(
          OutputProperty outputProperty, Mode mode){

    switch (outputProperty.getOutputPropertyType()) {
      case ROUTE_CHOICE_CONVERGENCE_GAP:
        return SimulationOutputTypeAdapter.getRouteChoiceConvergenceGap(getAssignment().getGapFunction());
      case ROUTE_CHOICE_ITERATION_RUN_TIME:
        return getRouteChoiceIterationRunTime();
      case PATH_COUNT:
        return getTotalPathCount(mode);
      case PATHS_ADDED:
        return getAddedPathCount(mode);
      case PATHS_REMOVED:
        return getRemovedPathCount(mode);
      default:
        return Optional.of(String.format("Tried to find property of %s which is not applicable for Simulation output", outputProperty.getName()));
    }
  }

  /**
   * Constructor
   *
   * @param outputType        the output type for the current persistence
   * @param trafficAssignment the traffic assignment used to provide the data
   */
  public SimulationOutputTypeAdapterImpl(OutputType outputType, TrafficAssignment trafficAssignment) {
    super(outputType, trafficAssignment);
  }

  /**
   * Returns the specified output property values for the simulation output property
   *
   * @param outputProperty the specified output property
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @return the value(s) of the specified property
   */
  @Override
  public Optional<?> getSimulationOutputPropertyValue(
          OutputProperty outputProperty,
          Mode mode,
          TimePeriod timePeriod) {
    try {

      // try output type independent results
      Optional<?> value = getOutputTypeIndependentPropertyValue(outputProperty, mode, timePeriod);
      if (!value.isPresent()) {
        // try simulation specific results
        value = getSimulationIndependentPropertyValue(outputProperty);
      }

      if (value.isPresent()) {
        return value;
      }

      // simulation dependent result
      return getSimulationDependentPropertyValue(outputProperty, mode);
      // no unit convertable types here, so do not verify if conversion is needed

    } catch (PlanItRunTimeException e) {
      return Optional.of(List.of(e.getMessage()));
    }
  }

}
