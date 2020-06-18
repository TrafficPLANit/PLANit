package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

public final class MaximumDensityOutputProperty extends BaseOutputProperty {

  public static final String NAME = "Maximum Density";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Units getUnits() {
    return Units.VEH_KM;
  }

  @Override
  public Type getType() {
    return Type.DOUBLE;
  }

  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.DENSITY;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.INPUT_PRIORITY;
  }

}
