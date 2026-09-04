package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

/**
 * output property for node geometry
 */
public final class DownstreamNodeGeometryOutputProperty extends OutputProperty {

  /** Public no-arg constructor required for reflective instantiation. */
  public DownstreamNodeGeometryOutputProperty(){}

  /** Downstream Node Geometry. */
  public static final String NAME = "Downstream Node Geometry";

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return NAME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Unit getDefaultUnit() {
    return Unit.SRS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataType getDataType() {
    return DataType.SRSNAME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.DOWNSTREAM_NODE_GEOMETRY;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
