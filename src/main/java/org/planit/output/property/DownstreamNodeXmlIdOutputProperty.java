package org.planit.output.property;

import org.planit.output.enums.DataType;
import org.planit.utils.unit.Unit;

public final class DownstreamNodeXmlIdOutputProperty extends OutputProperty {

  public static final String NAME = "Downstream Node Xml Id";

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
    return OutputPropertyType.DOWNSTREAM_NODE_XML_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
