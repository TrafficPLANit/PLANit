package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class NumberOfLanesOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Number of Lanes";

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
    return Type.INTEGER;
  }

  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.NUMBER_OF_LANES;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.INPUT_PRIORITY;
  }

}
