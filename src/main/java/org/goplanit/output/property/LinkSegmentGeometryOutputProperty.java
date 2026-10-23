package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

/**
 * Output property for Link Segment Geometry.
 */
public final class LinkSegmentGeometryOutputProperty extends OutputProperty {


  /** Link Segment Geometry. */
  public static final String NAME = "Link Segment Geometry";
  /**
   * Public no-arg constructor required for reflective instantiation.
   */
  public LinkSegmentGeometryOutputProperty(){}

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
    return OutputPropertyType.LINK_SEGMENT_GEOMETRY;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
