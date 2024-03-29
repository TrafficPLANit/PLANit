package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

public final class UpstreamNodeXmlIdOutputProperty extends OutputProperty {

  public static final String NAME = "Upstream Node Xml Id";

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
    return DataType.STRING;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.UPSTREAM_NODE_XML_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
