package org.planit.output.property;

import org.planit.output.enums.DataType;
import org.planit.utils.unit.Units;

/**
 * Density output property in pcu/km
 * 
 * @author markr
 *
 */
public final class DensityOutputProperty extends OutputProperty {

  public static final String NAME = "Density";

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
  public Units getDefaultUnits() {
    return Units.PCU_KM;
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
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
