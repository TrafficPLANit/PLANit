package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

public final class UpstreamNodeGeometryOutputProperty extends OutputProperty {

  public static final String NAME = "Upstream Node Geometry";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Unit getDefaultUnit() {
    return Unit.SRS;
  }

  @Override
  public DataType getDataType() {
    return DataType.SRSNAME;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.UPSTREAM_NODE_GEOMETRY;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.INPUT_PRIORITY;
  }

}
