package org.planit.output.property;

import org.planit.output.enums.DataType;
import org.planit.utils.unit.Unit;

public final class LinkSegmentIdOutputProperty extends OutputProperty {

  public static final String NAME = "Link Segment Id";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Unit getDefaultUnit() {
    return Unit.NONE;
  }

  @Override
  public DataType getDataType() {
    return DataType.LONG;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.LINK_SEGMENT_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
