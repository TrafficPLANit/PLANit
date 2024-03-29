package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

public final class NumberOfLanesOutputProperty extends OutputProperty {

  public static final String NAME = "Number of Lanes";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Unit getDefaultUnit() {
    return Unit.NONE;
  }

  @Override
  public DataType getDataType() {
    return DataType.INTEGER;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.NUMBER_OF_LANES;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.INPUT_PRIORITY;
  }

}
