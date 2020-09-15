package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.utils.unit.Units;

public final class DownstreamNodeExternalIdOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Downstream Node External Id";

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
    return OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.ID_PRIORITY;
  }

}
