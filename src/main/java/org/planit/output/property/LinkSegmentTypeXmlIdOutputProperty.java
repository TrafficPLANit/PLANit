package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.utils.unit.Units;

public final class LinkSegmentTypeXmlIdOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Link Segment Type XML Id";

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
    return OutputProperty.LINK_SEGMENT_TYPE_XML_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
