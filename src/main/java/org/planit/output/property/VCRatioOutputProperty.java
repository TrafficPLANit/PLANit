package org.planit.output.property;

import org.planit.output.enums.DataType;
import org.planit.utils.unit.Units;

/**
 * Calculate V/C ratio output property class
 * 
 * @author gman6028
 *
 */
public final class VcRatioOutputProperty extends OutputProperty {

  public static final String NAME = "VC Ratio";

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
    return Units.NONE;
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
    return OutputPropertyType.VC_RATIO;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
