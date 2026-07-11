package org.goplanit.output.property;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.output.enums.DataType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.unit.Unit;

import java.text.DecimalFormat;
import java.util.Optional;

/**
 * Output property for Route Choice Convergence Gap.
 */
public final class RouteChoiceConvergenceGapOutputProperty extends OutputProperty {


  /** Route Choice Convergence Gap. */
  public static final String NAME = "Route Choice Convergence Gap";
  /**
   * Public no-arg constructor required for reflective instantiation.
   */
  public RouteChoiceConvergenceGapOutputProperty(){}

  private final DecimalFormat decimalFormat = new DecimalFormat("#.00000000000000####");

  /** Convergence gap requires high precision when being outputted, so when requested to formatting we
   * impose a high precision of 14 decimals
   *
   * @param value the value to be output
   * @return formatted value
   */
  public String formatValue(Object value) {
    return decimalFormat.format(value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return NAME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Unit getDefaultUnit() {
    return Unit.NONE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataType getDataType() {
    return DataType.DOUBLE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.ROUTE_CHOICE_CONVERGENCE_GAP;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

  /**
   * Returns the current route choice convergence gap
   * 
   * @param trafficAssignment TrafficAssignment containing data which may be required
   * @return the current route choice convergence gap
   */
  public static Optional<Long> getRouteChoiceConvergenceGap(TrafficAssignment trafficAssignment){
    throw new PlanItRunTimeException("Route Choice Convergence Gap Property not yet implemented");
  }

}
