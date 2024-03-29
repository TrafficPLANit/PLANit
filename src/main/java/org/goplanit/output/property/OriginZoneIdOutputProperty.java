package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

public final class OriginZoneIdOutputProperty extends OutputProperty {

  public static final String NAME = "Origin Zone Id";

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
    return DataType.LONG;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.ORIGIN_ZONE_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
