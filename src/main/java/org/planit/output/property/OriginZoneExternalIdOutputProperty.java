package org.planit.output.property;

import org.planit.output.enums.DataType;
import org.planit.utils.unit.Units;

public final class OriginZoneExternalIdOutputProperty extends OutputProperty {

  public static final String NAME = "Origin Zone External Id";

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
    return DataType.STRING;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.ORIGIN_ZONE_EXTERNAL_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
