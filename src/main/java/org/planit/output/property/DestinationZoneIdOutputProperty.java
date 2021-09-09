package org.planit.output.property;

import org.planit.output.enums.DataType;
import org.planit.utils.unit.Units;

public final class DestinationZoneIdOutputProperty extends OutputProperty {

  public static final String NAME = "Destination Zone Id";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Units getDefaultUnits() {
    return Units.NONE;
  }

  @Override
  public DataType getDataType() {
    return DataType.LONG;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.DESTINATION_ZONE_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
