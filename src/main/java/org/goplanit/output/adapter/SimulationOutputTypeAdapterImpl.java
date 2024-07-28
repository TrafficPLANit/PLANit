package org.goplanit.output.adapter;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.od.path.OdMultiPathIterator;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.PathOutputIdentificationType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.path.ManagedDirectedPathImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.path.ManagedDirectedPath;
import org.goplanit.utils.time.TimePeriod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Top-level abstract class which defines the common methods required by simulation output type adapters
 * 
 * @author markr
 *
 */
public abstract class SimulationOutputTypeAdapterImpl extends OutputTypeAdapterImpl implements SimulationOutputTypeAdapter {

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
   * @return result (if any match)
   */
  protected Optional<?> getSimulationDependentPropertyValue(
          OutputProperty outputProperty){

    switch (outputProperty.getOutputPropertyType()) {
      case ROUTE_CHOICE_CONVERGENCE_GAP:
        return SimulationOutputTypeAdapter.getRouteChoiceConvergenceGap(getAssignment().getGapFunction());
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
        value = getSimulationDependentPropertyValue(outputProperty);
      }

      if (value.isPresent()) {
        return value;
      }

      // simulation dependent result
      return getSimulationDependentPropertyValue(outputProperty);
      // no unit convertable types here, so do not verify if conversion is needed

    } catch (PlanItRunTimeException e) {
      return Optional.of(List.of(e.getMessage()));
    }
  }

}
