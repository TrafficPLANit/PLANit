package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class LinkCostOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Cost";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Units getUnits() {
    return Units.H;
  }

  @Override
  public Type getType() {
    return Type.DOUBLE;
  }

  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.LINK_COST;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
