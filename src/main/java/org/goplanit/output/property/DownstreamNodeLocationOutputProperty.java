package org.goplanit.output.property;

import org.goplanit.output.enums.DataType;
import org.goplanit.utils.unit.Unit;

public final class DownstreamNodeLocationOutputProperty extends OutputProperty {

  public static final String NAME = "Downstream Node Location";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Unit getDefaultUnit() {
    return Unit.SRS;
  }

  @Override
  public DataType getDataType() {
    return DataType.SRSNAME;
  }

  @Override
  public OutputPropertyType getOutputPropertyType() {
    return OutputPropertyType.DOWNSTREAM_NODE_LOCATION;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.INPUT_PRIORITY;
  }

}
