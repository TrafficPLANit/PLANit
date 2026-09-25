package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

/**
 * Output property for Upstream Node Geometry.
 */
public final class UpstreamNodeGeometryOutputProperty extends OutputProperty {

  /** Public no-arg constructor required for reflective instantiation. */
  public UpstreamNodeGeometryOutputProperty(){}


  /** Upstream Node Geometry. */
  public static final String NAME = "Upstream Node Geometry";

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
    return OutputPropertyType.UPSTREAM_NODE_GEOMETRY;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
