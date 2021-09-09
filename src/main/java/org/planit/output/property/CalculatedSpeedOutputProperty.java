package org.planit.output.property;

import org.planit.output.enums.DataType;
import org.planit.utils.unit.Units;

/**
 * Calculate speed output property class
 * 
 * @author gman6028
 *
 */
public final class CalculatedSpeedOutputProperty extends OutputProperty {

  public static final String NAME = "Calculated Speed";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Units getDefaultUnits() {
    return Units.KM_HOUR;
  }

  @Override
  public DataType getDataType() {
    return DataType.DOUBLE;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.CALCULATED_SPEED;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
