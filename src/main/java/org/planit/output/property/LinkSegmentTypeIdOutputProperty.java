package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.utils.unit.Units;

public final class LinkSegmentTypeIdOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Link Segment Type Id";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Units getUnits() {
    return Units.NONE;
  }

  @Override
  public Type getType() {
    return Type.STRING;
  }

  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.LINK_SEGMENT_TYPE_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
