package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

/**
 * Number of paths relevant to context
 */
public final class PathCountOutputProperty extends OutputProperty {

  public static final String NAME = "Path count";

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
    return OutputPropertyType.PATH_COUNT;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
