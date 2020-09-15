package org.planit.output.property;

import org.planit.output.enums.Type;
import org.planit.utils.unit.Units;

/**
 * Cost times flow output property class
 * 
 * @author gman6028
 *
 */
public final class CostTimesFlowOutputProperty extends BaseOutputProperty {

  public static final String NAME = "X_Cost x Flow";

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
    return Type.DOUBLE;
  }

  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.COST_TIMES_FLOW;
  }

  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
