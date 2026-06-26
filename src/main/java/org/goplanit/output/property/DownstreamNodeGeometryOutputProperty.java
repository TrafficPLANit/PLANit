package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

/**
 * output property for node geometry
 */
public final class DownstreamNodeGeometryOutputProperty extends OutputProperty {

  /** dummy constructor */
  public DownstreamNodeGeometryOutputProperty(){}

  /** name */
  public static final String NAME = "Downstream Node Geometry";

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
    return OutputPropertyType.DOWNSTREAM_NODE_GEOMETRY;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
