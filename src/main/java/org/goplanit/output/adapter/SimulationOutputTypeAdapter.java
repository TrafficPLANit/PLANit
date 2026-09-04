package org.goplanit.output.adapter;

import org.goplanit.gap.GapFunction;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;

import java.util.Optional;

/**
 * Output type adapter interface for simulation (per iteration). Provide convenience access
 * to output properties that are generally supported for this output type
 * 
 * @author markr
 *
 */
public interface SimulationOutputTypeAdapter extends OutputTypeAdapter {

  /**
   * Collect the route choice convergence gap based on the provided gap function
   *
   * @param gapFunction to extract gap from
   * @return gap found
   */
  public static Optional<Double> getRouteChoiceConvergenceGap(GapFunction gapFunction) {
    return Optional.of(gapFunction.getGap());
  }

  /**
   * Returns the specified output property values for the simulation output property
   *
   * @param outputProperty the specified output property
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @return the value(s) of the specified property
   */
  public abstract Optional<?> getSimulationOutputPropertyValue(
      OutputProperty outputProperty,
      Mode mode,
      TimePeriod timePeriod);

}
