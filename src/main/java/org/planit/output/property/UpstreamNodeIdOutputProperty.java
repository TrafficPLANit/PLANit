package org.planit.output.property;

import org.planit.output.enums.DataType;
import org.planit.utils.unit.Units;

public final class UpstreamNodeIdOutputProperty extends OutputProperty {

  public static final String NAME = "Upstream Node Id";

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
    return OutputPropertyType.UPSTREAM_NODE_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
