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

  public static final String NAME = "Cost_X_Flow";

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return NAME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Units getUnits() {
    return Units.NONE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return Type.DOUBLE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputProperty getOutputProperty() {
    return OutputProperty.COST_TIMES_FLOW;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputPropertyPriority getColumnPriority() {
    return OutputPropertyPriority.RESULT_PRIORITY;
  }

}
