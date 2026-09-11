package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

/**
 * Maximum density in pcu/km
 * 
 * @author markr
 *
 */
public final class MaximumDensityOutputProperty extends OutputProperty {


  /** Maximum Density. */
  public static final String NAME = "Maximum Density";
  /**
   * Public no-arg constructor required for reflective instantiation.
   */
  public MaximumDensityOutputProperty(){}

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
    return Unit.PCU_KM;
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
    return OutputPropertyType.DENSITY;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.INPUT_PRIORITY;
  }

}
