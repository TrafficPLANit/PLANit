package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class DownstreamNodeLocationOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Downstream Node Location";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Units getUnits() {
    return Units.SRS;
  }

  @Override
  public Type getType() {
    return Type.SRSNAME;
  }

  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.DOWNSTREAM_NODE_LOCATION;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.INPUT_PRIORITY;
  }

}
