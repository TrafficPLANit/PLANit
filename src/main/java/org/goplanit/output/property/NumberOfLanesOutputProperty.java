package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

/**
 * Output property for Number of Lanes.
 */
public final class NumberOfLanesOutputProperty extends OutputProperty {


  /** Number of Lanes. */
  public static final String NAME = "Number of Lanes";
  /**
   * Public no-arg constructor required for reflective instantiation.
   */
  public NumberOfLanesOutputProperty(){}

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
    return DataType.INTEGER;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.NUMBER_OF_LANES;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.INPUT_PRIORITY;
  }

}
