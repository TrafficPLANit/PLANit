package org.planit.output.property;

import org.planit.output.enums.DataType;
import org.planit.utils.unit.Units;

public final class UpstreamNodeLocationOutputProperty extends OutputProperty {

  public static final String NAME = "Upstream Node Location";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Units getDefaultUnits() {
    return Units.SRS;
  }

  @Override
  public DataType getDataType() {
    return DataType.SRSNAME;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.UPSTREAM_NODE_LOCATION;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.INPUT_PRIORITY;
  }

}
