package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public class LinkSegmentExternalIdOutputProperty extends BaseOutputProperty {

  public final static String NAME = "Link Segment External Id";

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
    return Type.LONG;
  }

  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.LINK_SEGMENT_EXTERNAL_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
