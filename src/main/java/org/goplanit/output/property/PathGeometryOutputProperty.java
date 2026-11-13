package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

/**
 * Output property for PathGeometry.
 */
public final class PathGeometryOutputProperty extends OutputProperty {


  /** PathGeometry. */
  public static final String NAME = "PathGeometry";
  /**
   * Public no-arg constructor required for reflective instantiation.
   */
  public PathGeometryOutputProperty(){}

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
    return OutputPropertyType.PATH_GEOMETRY;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
