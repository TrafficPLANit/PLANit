package org.planit.output.property;

import org.planit.output.enums.DataType;
import org.planit.utils.unit.Unit;

public final class PathOutputStringProperty extends OutputProperty {

  public static final String NAME = "Path";

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
    return DataType.STRING;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.PATH_STRING;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
