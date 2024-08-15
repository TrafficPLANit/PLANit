package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

/**
 * Property to provide the run time used for completing a route choice iteration in seconds.
 * 
 * @author markr
 *
 */
public final class RouteChoiceIterationRunTimeOutputProperty extends OutputProperty {

  /**
   * Name of the property
   */
  public static final String NAME = "Route choice iteration run time";

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean supportsUnitOverride() {
    return true;
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
    return Unit.MILLISECOND;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataType getDataType() {
    return DataType.LONG;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.ROUTE_CHOICE_ITERATION_RUN_TIME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
